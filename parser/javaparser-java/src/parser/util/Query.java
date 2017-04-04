// $Id$
// Created by Balint Kiss, 2017

package parser.util;

import parser.entity.File;
import parser.entity.JavaFunction;
import parser.entity.JavaType;
import parser.entity.Project;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public final class Query {

  /**
   * Disallow construction
   */
  private Query() {}

  public static Project getOrCreateProject(final EntityManager em, final long id) {
    Project project = em.find(Project.class, id);

    if (project == null) {
      project = new Project(id);
      em.persist(project);
    }

    return project;
  }

  public static File getFileByName(final EntityManager em, final String name) {
    TypedQuery<File> q = em.createQuery("SELECT f FROM File f WHERE f.path like \"" + name + "\"", File.class);
    return findFile(q, name);
  }

  public static File getOrCreateDirectory(
          final EntityManager em,
          final Project project,
          String path,
          final long timestamp) {
    List<String> dirs = new LinkedList<>(Arrays.asList(path.split("/")));
    path = "/"; // replace path with the root
    File parent = getFileByName(em, path);

    if (parent == null) {
      // the first 'parent' is the root

      parent = new File();
      parent.setPath(path);
      parent.setFilename("");
      parent.setType(File.DIRECTORY);
      parent.setProject(project);
      parent.setParent(null);
      parent.setTimestamp(timestamp);
      em.persist(parent);
    }

    while (!dirs.isEmpty()) {
      String name = dirs.get(0);
      path = path + (path.length() > 1 ? "/" : "") + name;
      dirs.remove(0);     // pop dir

      File next_dir = getFileByName(em, path);

      if (next_dir == null) {
        next_dir = new File();
        next_dir.setPath(path);
        next_dir.setFilename(name);
        next_dir.setType(File.DIRECTORY);
        next_dir.setProject(project);
        next_dir.setParent(parent);
        next_dir.setTimestamp(timestamp);
        em.persist(next_dir);
      }

      parent = next_dir;
    }

    return parent;
  }

  public static File findFile(TypedQuery<File> fs, String path) {
    if (fs != null) {
      for (File f : fs.getResultList()) {
        if (path.equals(f.getPath())) {
          return f;
        }
      }
    }

    return null;
  }

  public static JavaType findType(TypedQuery<JavaType> ts, String qualName) {
    if (ts != null) {
      for (JavaType t : ts.getResultList()) {
        if (qualName.equals(t.getQualifiedName())) {
          return t;
        }
      }
    }

    return null;
  }

  public static JavaFunction findFunction(TypedQuery<JavaFunction> fs, String mangledName) {
    if (fs != null) {
      for (JavaFunction f : fs.getResultList()) {
        if (mangledName.equals(f.getMangledName())) {
          return f;
        }
      }
    }

    return null;
  }
}
