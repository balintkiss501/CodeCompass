// $Id$

package parser.entity;

import javax.persistence.*;

@Entity
@Table(name = "\"BuildLog\"")
public class BuildLog implements java.io.Serializable {
  private static final long serialVersionUID = 1570473137245383463L;

  public BuildLog() {}

  public class Type {
    public static final int UNKNOWN = 0;
    public static final int ERROR = 1;
    public static final int FATAL_ERROR = 2;
    public static final int WARNING = 3;
    public static final int NOTE = 4;
    public static final int CODING_RULE = 5;
  }

  @Column(name = "id")
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO, generator = "BuildLog_id_seq")
  @SequenceGenerator(name = "BuildLog_id_seq", sequenceName = "\"BuildLog_id_seq\"")
  long id;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @JoinColumn(name = "action", nullable = false)
  parser.entity.BuildAction action;

  public parser.entity.BuildAction getAction() {
    return action;
  }

  public void setAction(parser.entity.BuildAction action) {
    this.action = action;
  }

  @Column(name = "log_message", nullable = false)
  String log_message;

  public String getLog_message() {
    return log_message;
  }

  public void setLog_message(String log_message) {
    this.log_message = log_message;
  }

  @Column(name = "log_type")
  int type;

  public int getType() {
    return type;
  }

  public void setType(int type) {
    this.type = type;
  }

  @JoinColumn(name = "location_file")
  @ManyToOne
  parser.entity.File file;

  public parser.entity.File getFile() {
    return file;
  }

  public void setFile(parser.entity.File file) {
    this.file = file;
  }

  @Column(name = "location_range_start_line")
  int loc_start_line;

  public int getLoc_start_line() {
    return loc_start_line;
  }

  public void setLoc_start_line(int loc_start_line) {
    this.loc_start_line = loc_start_line;
  }

  @Column(name = "location_range_start_column")
  int loc_start_col;

  public int getLoc_start_col() {
    return loc_start_col;
  }

  public void setLoc_start_col(int loc_start_col) {
    this.loc_start_col = loc_start_col;
  }

  @Column(name = "location_range_end_line")
  int loc_end_line;

  public int getLoc_end_line() {
    return loc_end_line;
  }

  public void setLoc_end_line(int loc_end_line) {
    this.loc_end_line = loc_end_line;
  }

  @Column(name = "location_range_end_column")
  int loc_end_col;

  public int getLoc_end_col() {
    return loc_end_col;
  }

  public void setLoc_end_col(int loc_end_col) {
    this.loc_end_col = loc_end_col;
  }
}
