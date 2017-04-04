package parser.database;

import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.config.TargetServer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.spi.PersistenceUnitTransactionType;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for creating JPA entity manager.
 */
public final class EMCreator {
  private static final Logger log = Logger.getLogger(EMCreator.class.getName());

  public static final String PREFIX_SQLITE = "sqlite";
  public static final String PREFIX_PGSQL_JDBC = "postgresql";  // JDBC pgsql prefix
  public static final String PREFIX_PGSQL_CUS = "pgsql";        // custom pgsql prefix

  public static final String DRIVER_SQLITE = "org.sqlite.JDBC";
  public static final String DRIVER_PGSQL = "org.postgresql.Driver";

  /**
   * Disallow construction.
   */
  private EMCreator() {}

  /**
   * Set up database connection and create a new JPA entity manager.
   * The database can either be SQLite or PostgreSQL.
   *
   * @param obj   Object to use it's classloader when creating entity manager.
   * @param csp   Database connection string parser that already read the database connection string
   * @return
   */
  public static EntityManager createEntityManager(final Object obj, final ConnStringParser csp) {
    // http://www.eclipse.org/forums/index.php?t=msg&goto=487011
    // http://wiki.eclipse.org/EclipseLink/Examples/JPA/OutsideContainer
    // http://www.eclipse.org/eclipselink/documentation/2.5/solutions/testingjpa.htm#BABEBCCJ

    Map<String, Object> props = new java.util.HashMap<>();

    props.put(PersistenceUnitProperties.CLASSLOADER, obj.getClass().getClassLoader());

    props.put(PersistenceUnitProperties.TRANSACTION_TYPE, PersistenceUnitTransactionType.RESOURCE_LOCAL.name());

    String jdbcUrl = "jdbc:" + csp.getUrl();

    props.put(PersistenceUnitProperties.JDBC_DRIVER, csp.getDriver());
    props.put(PersistenceUnitProperties.JDBC_URL, jdbcUrl);
    props.put(PersistenceUnitProperties.JDBC_USER, csp.getUser());
    props.put(PersistenceUnitProperties.JDBC_PASSWORD, csp.getPassword());

    props.put(PersistenceUnitProperties.TARGET_SERVER, TargetServer.None);

    String databasePlatform;

    if (PREFIX_PGSQL_JDBC.equals(csp.getPrefix())) {
      databasePlatform = "org.eclipse.persistence.platform.database.PostgreSQLPlatform";
    } else {
      databasePlatform = "DatabasePlatform";
    }

    props.put(PersistenceUnitProperties.TARGET_DATABASE, databasePlatform);

    try {
      // Just for sure, store the unnecessary drivers in a list.

      String driver = csp.getDriver();
      List<Driver> driversToDrop = new ArrayList<>();

      for (Enumeration<Driver> e = java.sql.DriverManager.getDrivers(); e.hasMoreElements(); ) {
        java.sql.Driver drv = e.nextElement();

        if (!driver.equals(drv.getClass().getName())) {
          driversToDrop.add(drv);
        }
      }

      // Remove all drivers in 'driversToDrop'.
      for (java.sql.Driver drv : driversToDrop) {
        log.log(Level.FINEST, "Deregister driver: " + drv.getClass().getName());
        java.sql.DriverManager.deregisterDriver(drv);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }

    EntityManagerFactory emf = Persistence.createEntityManagerFactory("ParserPU", props);

    return emf.createEntityManager();
  }
}
