// $Id$
// Created by Balint Kiss, 2017

package parser.util;

import parser.entity.File;

/**
 * Utilities for creating hashes from inputs.
 */
public final class Hashing {

  /**
   * Disallow construction.
   */
  private Hashing() {}

  /**
   * Create Fowler–Noll–Vo hash from input.
   *
   * @param str
   * @return
   */
  public static long fnvHash(final String str) {
    long hash = 0xCBF29CE484222325L;

    for (int i = 0, n = str.length(); i < n; ++i) {
      hash ^= (long) str.charAt(i);
      hash *= 1099511628211L;
    }

    // We have to avoid negative numbers.
    return hash & 0x7fffffffffffffffL;
  }

  /**
   * Create SHA-1 hash from contents of a file.
   *
   * @param content
   * @return
   */
  public static String computeContentHash(final char[] content) {
    try {
      java.security.MessageDigest sha1 = java.security.MessageDigest.getInstance("SHA-1");
      sha1.reset();
      sha1.update(content.toString().getBytes("utf8"));

      StringBuilder buf = new StringBuilder(64);

      for (byte b : sha1.digest()) {
        buf.append(String.format("%02x", ((int) b) & 0xff));
      }

      return buf.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Create Fowler–Noll–Vo hash as id of an AST node that's in a file.
   *
   * @param val
   * @param mangledName
   * @param file
   * @param start
   * @param end
   * @return
   */
  public static long permaAstId(
          final String val,
          final String mangledName,
          final File file,
          final int start,
          final int end) {
    String id = val + ":" + file.getId() + ":" + start + ":" + end + ":" + mangledName;
    return fnvHash(id);
  }

  /**
   * Create Fowler–Noll–Vo hash as id of an AST node that doesn't exist in a file.
   *
   * @param val
   * @param mangledName
   * @return
   */
  public static long permaAstId(final String val, final String mangledName) {
    String id = val + ":not_in_file:" + mangledName;
    return fnvHash(id);
  }

  /**
   * Create Fowler–Noll–Vo hash from name of an AST node.
   *
   * @param str
   * @return
   */
  public static long getNameHash(final String str) {
    return fnvHash(str);
  }

  /**
   * Create Fowler–Noll–Vo hash from HTML document.
   *
   * @param str
   * @return
   */
  public static long getDocHtmlHash(final String str) {
    return fnvHash(str);
  }
}
