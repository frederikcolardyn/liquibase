package liquibase.sqlgenerator.core;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Date;

import liquibase.database.Database;
import liquibase.database.core.CassandraDatabase;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.exception.ValidationErrors;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.core.LockDatabaseChangeLogStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.util.NetUtil;

public class LockDatabaseChangeLogGenerator extends AbstractSqlGenerator<LockDatabaseChangeLogStatement> {

	public ValidationErrors validate(LockDatabaseChangeLogStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
		return new ValidationErrors();
	}

	private static String hostname;
	private static String hostaddress;

	static {
		InetAddress localHost;
		try {
			localHost = NetUtil.getLocalHost();
			hostname = localHost.getHostName();
			hostaddress = localHost.getHostAddress();
		} catch (Exception e) {
			throw new UnexpectedLiquibaseException(e);
		}
	}

	public Sql[] generateSql(LockDatabaseChangeLogStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
		String liquibaseSchema = database.getLiquibaseSchemaName();
		String liquibaseCatalog = database.getLiquibaseCatalogName();

		UpdateStatement updateStatement = new UpdateStatement(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogLockTableName());
		updateStatement.addNewColumnValue("LOCKED", true);
		updateStatement.addNewColumnValue("LOCKEDBY", hostname + " (" + hostaddress + ")");

		if (database instanceof CassandraDatabase) {
			updateStatement.addNewColumnValue("LOCKGRANTED", System.currentTimeMillis());
			updateStatement.setWhereClause(database.escapeColumnName(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogTableName(), "ID") + " = 1");
		} else {
			updateStatement.addNewColumnValue("LOCKGRANTED", new Timestamp(new java.util.Date().getTime()));
			updateStatement.setWhereClause(database.escapeColumnName(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogTableName(), "ID")
					+ " = 1 AND " + database.escapeColumnName(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogTableName(), "LOCKED") + " = "
					+ DataTypeFactory.getInstance().fromDescription("boolean").objectToSql(false, database));
		}

		return SqlGeneratorFactory.getInstance().generateSql(updateStatement, database);

	}
}