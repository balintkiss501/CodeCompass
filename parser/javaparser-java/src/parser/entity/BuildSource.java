// $Id$

package parser.entity;

import javax.persistence.*;

@Entity
@Table(name = "\"BuildSource\"")
public class BuildSource implements java.io.Serializable {
  private static final long serialVersionUID = 1570473935355383463L;

  public BuildSource() {}

  @Column(name = "id")
  @Id
  @GeneratedValue
  long id;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @JoinColumn(name = "file", nullable = false)
  parser.entity.File file;

  public parser.entity.File getFile() {
    return file;
  }

  public void setFile(parser.entity.File file) {
    this.file = file;
  }

  @JoinColumn(name = "action", nullable = false)
  parser.entity.BuildAction action;

  public parser.entity.BuildAction getAction() {
    return action;
  }

  public void setAction(parser.entity.BuildAction action) {
    this.action = action;
  }
}
