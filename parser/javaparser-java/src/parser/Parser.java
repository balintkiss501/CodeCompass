// $Id$
// Created by Aron Barath, 2013

package parser;

import cc.parser.JavaParserArg;
import cc.parser.JavaParsingResult;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import parser.database.ConnStringParser;
import parser.database.EMCreator;
import parser.entity.Option;
import parser.entity.Project;
import parser.exception.ParseException;
import parser.jarparser.JarParser;
import parser.sourceparser.ArgParser;
import parser.sourceparser.AstVisitor;
import parser.util.Common;
import parser.util.ProblemHandler;
import parser.util.Query;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO: decouple database operations from parsing
public class Parser {
  private static final Logger log = Logger.getLogger(Parser.class.getName());

  private EntityManager em;

  public JavaParsingResult parse(JavaParserArg arg) {
    ArgParser ap;

    // FIXME: Should use the received arg instead of build a command line argument.
    List<String> args = new ArrayList<>();
    args.add("--db");
    args.add(arg.database);
    args.add("--rt");
    args.add(arg.rtJar);
    args.add("--buildid");
    args.add(arg.buildId);
    args.add("-sourcepath");
    args.add(arg.sourcepath);
    args.addAll(arg.opts);

    try {
      ap = new ArgParser(args.toArray(new String[args.size()]));
    } catch (Exception ex) {
      System.out.println("Bad arguments.");
      return JavaParsingResult.Fail;
    }

    String database = ap.getDatabase();
    if (database.isEmpty()) {
      // fail-safe sqlite database
      database = EMCreator.PREFIX_SQLITE + ":database=javaparser.sqlite";
    } else if (database.endsWith(".sqlite") && !database.startsWith(EMCreator.PREFIX_SQLITE)) {
      // if database is not a proper sqlite connection string, then extend it
      database = EMCreator.PREFIX_SQLITE + ":database=" + database;
    }

    if (!ap.isGood()) {
      System.out.println("Bad arguments.");
      return JavaParsingResult.Fail;
    }

    log.log(Level.FINEST, "Orig.db.: " + database);

    ConnStringParser csp = new ConnStringParser("test");
    csp.addPrefix(EMCreator.PREFIX_SQLITE, EMCreator.DRIVER_SQLITE);
    csp.addPrefix(EMCreator.PREFIX_PGSQL_JDBC, EMCreator.DRIVER_PGSQL);
    csp.addAlias(EMCreator.PREFIX_PGSQL_CUS, EMCreator.PREFIX_PGSQL_JDBC);
    if (!csp.parse(database)) {
      return JavaParsingResult.Fail;
    }

    String filename = ap.getFilename();
    System.out.println("Parsing " + filename);

    final char[] source = loadContent(filename);
    if (source == null) {
      System.out.println("File open error: \"" + filename + "\".");
      return JavaParsingResult.Fail;
    }

    log.log(Level.FINEST, "Source:");
    log.log(Level.FINEST, "----------------------------------------------------");
    log.log(Level.FINEST, new String(source));
    log.log(Level.FINEST, "----------------------------------------------------");

    String driver = csp.getDriver();

    log.log(Level.FINEST, "Driver:   " + driver);
    log.log(Level.FINEST, "Database: " + database);

    String[] classpath = ap.getClassPathArray();
    String[] sourcepath = ap.getSourcePathArray();

    log.log(Level.FINEST, "Classpath:");
    for (String s : classpath) {
      log.log(Level.FINEST, "    " + s);
    }
    log.log(Level.FINEST, "end of classpath");

    log.log(Level.FINEST, "Sourcepath:");
    for (String s : sourcepath) {
      log.log(Level.FINEST, "    " + s);
    }
    log.log(Level.FINEST, "end of sourcepath");

    // Traverse classpath for JAR files
    parseJars(csp, ap, classpath, sourcepath);

    // Parse source file
    return parseSource(csp, ap, classpath, sourcepath, filename, source);
  }

  /**
   * Create timestamp for database.
   *
   * @return    elapsed seconds since midnight, January 1, 1970 UTC
   */
  private long now() {
    return System.currentTimeMillis() / 1000L;
  }

  private void createNameOptionIfNotExist(String value) {   // FIXME: parameter "value" is never used
    TypedQuery<Option> qs = em.createQuery("SELECT o FROM Option o WHERE o.key like \"name\"", Option.class);

    if (qs == null || qs.getResultList().isEmpty()) {
      Option opt = new Option();
      opt.setKey("name");
      opt.setValue("");
      em.persist(opt);
    }
  }

  /**
   * Read contents of a file, for example source of .java file.
   *
   * @param filename    Name of file to get contents of
   * @return            Contents of file as array of characters (for Eclipse JDT input).
   */
  private char[] loadContent(final String filename) {
    try {
      FileReader reader = new FileReader(filename);
      StringBuilder builder = new StringBuilder();
      int initial_length = 256;
      char[] cbuf = new char[initial_length];
      int read_chars;

      while ((read_chars = reader.read(cbuf)) > 0) {
        if (read_chars != initial_length) {
          char[] copy = new char[read_chars];
          System.arraycopy(cbuf, 0, copy, 0, read_chars);
          builder.append(copy);
        } else {
          builder.append(cbuf);
        }
      }

      reader.close();

      return builder.toString().toCharArray();
    } catch (Exception ex) {
      return null;
    }
  }

  private int processProblems(int exitStatus, ProblemHandler ph) {
    if (0 < ph.getProblems().size()) {
      log.log(Level.FINEST, "There were " + ph.getProblems().size() + " problems.");

      for (ProblemHandler.Problem problem : ph.getProblems()) {
        if (problem.isError()) {
          exitStatus = 10;
        }

        log.log(Level.FINEST, "    " + problem.getProblemKind() + ": " + problem.getMessage());
      }
    }

    return exitStatus;
  }

  /**
   * Parse and persist Java source file.
   */
  private JavaParsingResult parseSource(
          final ConnStringParser csp,
          ArgParser ap,
          String[] classpath,
          String[] sourcepath,
          String filename,
          char[] source) {
    ASTParser astParser = ASTParser.newParser(AST.JLS4);

    em = EMCreator.createEntityManager(astParser, csp);
    em.getTransaction().begin();

    final long projectID = 1L; // this must be 1L
    Project project = Query.getOrCreateProject(em, projectID);

    int exitStatus = 0;

    String[] encodings = new String[sourcepath.length];
    for (int i = 0; i < encodings.length; ++i) {
      encodings[i] = "UTF-8";
    }

    astParser.setSource(source);
    Map<?, ?> options = JavaCore.getOptions();
    Common.applySourceLevel(ap.getSourceLevel(), options);
    astParser.setCompilerOptions(options);
    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);
    astParser.setUnitName(filename);
    astParser.setKind(ASTParser.K_COMPILATION_UNIT);
    astParser.setEnvironment(classpath, sourcepath, encodings, false);

    try {
      CompilationUnit cu = (CompilationUnit) astParser.createAST(null/*new org.eclipse.core.runtime.NullProgressMonitor()*/);

      if (cu != null) {
        // if there is no "name" option, then create one
        createNameOptionIfNotExist("");

        if (-1 == filename.lastIndexOf('/')) {
          filename = ap.getWorkingDir() + "/" + filename;
        }

        int last_sep = filename.lastIndexOf('/');
        int path_sep = last_sep;

        while (path_sep > 0 && filename.charAt(path_sep - 1) == '/') {
          --path_sep;
        }

        ProblemHandler ph = new ProblemHandler();

        cu.accept(new AstVisitor(
                em,
                project,
                cu,
                ph,
                filename.substring(0, path_sep),
                filename.substring(last_sep + 1),
                source,
                now(),
                ap.getBuildId(),
                ap.getCreateBuildAction()));

        exitStatus = processProblems(exitStatus, ph);
      }

      log.log(Level.FINEST, "~~~~~~~~~~~~~~~~~~~~~~~~~~");

      em.flush();
      em.getTransaction().commit();
      em.close();

      log.log(Level.FINEST, "Done.");

      if (exitStatus == 0)
        return JavaParsingResult.Success;
      else
        return JavaParsingResult.Fail;
    } catch (ParseException ex) {
      em.getTransaction().rollback();
      em.close();
      System.out.println("ERROR: " + ex.getMessage());
      return JavaParsingResult.Fail;
    } catch (Exception ex) {
      em.getTransaction().rollback();
      em.close();
      System.out.println("An exception has been caught.");
      ex.printStackTrace();
      return JavaParsingResult.Fail;
    }
  }

  /**
   * Traverse and parse JAR files from classpath. This method excludes the runtime library (RTjar) of Ant.
   *
   * @param classpath
   * @param ap
   */
  private void parseJars(
          final ConnStringParser csp,
          final ArgParser ap,
          final String[] classpath,
          final String[] sourcepath) {
    // Remove duplicates from classpath
    final String[] flattenedClasspath = Common.flatten(String.class, classpath);

    for (String classpathEntry : flattenedClasspath) {
      if (classpathEntry.endsWith(".jar") && !classpathEntry.endsWith(ap.getRuntimeLib())) {
        if (-1 == classpathEntry.lastIndexOf('/')) {
          classpathEntry = ap.getWorkingDir() + "/" + classpathEntry;
        }

        int jarLastSep = classpathEntry.lastIndexOf('/');
        int jarPathSep = jarLastSep;

        while (jarPathSep > 0 && classpathEntry.charAt(jarPathSep - 1) == '/') {
          --jarPathSep;
        }

        JarParser jarParser = new JarParser(
                csp,
                ap,
                flattenedClasspath,
                sourcepath,
                classpathEntry.substring(0, jarPathSep),
                classpathEntry.substring(jarLastSep + 1),
                now());
        jarParser.parseJar();
      }
    }
  }

}