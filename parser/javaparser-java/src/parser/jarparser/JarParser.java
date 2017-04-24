package parser.jarparser;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import parser.database.ConnStringParser;
import parser.database.EMCreator;
import parser.entity.File;
import parser.entity.Project;
import parser.exception.ParseException;
import parser.sourceparser.ArgParser;
import parser.sourceparser.AstVisitor;
import parser.util.Common;
import parser.util.ProblemHandler;
import parser.util.Query;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

// FIXME: persisted Jar files and package hierarchy won't have hashed ids
/**
 * Class for parsing classes inside JAR file.
 * It persist the JAR file as an openable directory in the database,
 * forms directory structure from package hierarchy, recovers all class files,
 * then persist them into database.
 */
public class JarParser {
  private ConnStringParser csp;
  private ArgParser ap;
  private String[] classpath;
  private String[] sourcepath;
  private String parentPath;
  private String jarFilename;
  private long timestamp;

  private EntityManager em;
  private Project project;
  private String absoluteJarPath;

  public JarParser(
          final ConnStringParser csp,
          final ArgParser ap,
          final String[] classpath,
          final String[] sourcepath,
          final String parentPath,
          final String jarFilename,
          final long timestamp) {
    this.csp = csp;
    this.ap = ap;
    this.classpath = classpath;
    this.sourcepath = sourcepath;
    this.parentPath = parentPath;
    this.jarFilename = jarFilename;
    this.timestamp = timestamp;
  }

  /**
   * Persist JAR as directory in database, then start parsing of all classes inside JAR.
   */
  public void parseJar() {
    em = EMCreator.createEntityManager(this, csp);
    em.getTransaction().begin();

    final long projectID = 1L; // this must be 1L
    project = Query.getOrCreateProject(em, projectID);

    // Resolve parent path from database and JAR if exists already
    File parent = Query.getOrCreateDirectory(em, project, parentPath, timestamp);
    File jarFile = Query.getFileByName(em, parentPath + "/" + jarFilename);

    // If JAR file doesn't exist yet, create it as directory and traverse its contents
    if (jarFile == null) {
      absoluteJarPath = parentPath + '/' + jarFilename;

      // Persist JAR file as directory
      jarFile = new File();
      jarFile.setPath(absoluteJarPath);
      jarFile.setFilename(jarFilename);
      jarFile.setProject(project);
      jarFile.setParent(parent);
      jarFile.setContent(null);
      jarFile.setTimestamp(timestamp);
      jarFile.setType(File.DIRECTORY);
      em.persist(jarFile);

      em.flush();
      em.getTransaction().commit();

      // Traverse JAR file while parsing classes inside it
      traverseJar(absoluteJarPath);
    }
  }

  /**
   * Iterate over classes inside JAR.
   *
   * @param absoluteJarPath
   */
  private void traverseJar(final String absoluteJarPath) {
    try (JarFile jar = new JarFile(absoluteJarPath)) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();

        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
          parseClass(entry);
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Parse class and persist it into database. This also starts the parsing and building of AST tree
   * from the recovered source.
   *
   * @param entry
   */
  private void parseClass(final JarEntry entry) {
    final String[] fileparts = entry.getName().split("/");
    final String classFilename = fileparts[fileparts.length - 1];

    String packageHierarchy = "";
    if (1 < fileparts.length) {
      packageHierarchy = StringUtils.join(Arrays.copyOfRange(fileparts, 0, fileparts.length - 1), '/');
    }

    final String classParentPath = absoluteJarPath + '/' + packageHierarchy;
    final String classFilePath = classParentPath + '/' + classFilename;

    File classFile = Query.getFileByName(em, classFilePath);
    if (classFile == null) {
      try {
        JavaClass javaClass = new ClassParser(absoluteJarPath, entry.getName()).parse();
        final char[] disassembledSource = new DisassembleVisitor(javaClass).getDecompiledCode();
        parseRecoveredSource(classParentPath, classFilename, disassembledSource);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Build AST from recovered source.
   *
   * @param classParentPath
   * @param filename
   * @param source
   */
  private void parseRecoveredSource(
          final String classParentPath,
          final String filename,
          final char[] source) {
    ASTParser astParser = ASTParser.newParser(AST.JLS4);

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

    CompilationUnit cu = (CompilationUnit) astParser.createAST(null/*new org.eclipse.core.runtime.NullProgressMonitor()*/);

    if (cu != null) {
      ProblemHandler ph = new ProblemHandler();

      em.getTransaction().begin();

      try {
        cu.accept(new AstVisitor(
                em,
                project,
                cu,
                ph,
                classParentPath,
                filename,
                source,
                timestamp,
                null,
                true));

        em.flush();
        em.getTransaction().commit();
      } catch (ParseException ex) {
        em.getTransaction().rollback();
        em.close();
        System.out.println("ERROR: " + ex.getMessage());
      } catch (Exception ex) {
        em.getTransaction().rollback();
        em.close();
        System.out.println("An exception has been caught.");
        ex.printStackTrace();
      }
    }
  }
}
