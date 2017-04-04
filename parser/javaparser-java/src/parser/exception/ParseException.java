// $Id$
// Created by Aron Barath, 2014

package parser.exception;

public class ParseException extends Exception {
  private static final long serialVersionUID = -2059831230073491075L;

  public ParseException(final String message) {
    this.message = message;
  }

  private String message;

  public String getMessage() {
    return message;
  }
}
