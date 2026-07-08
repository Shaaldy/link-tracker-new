package by.shaaldy.scrapper.migration;

import java.sql.Connection;
import javax.sql.DataSource;
import liquibase.Scope;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Собственноручный запуск миграций Liquibase (требование ДЗ: автозапуск через
 * spring-boot-starter отключён в конфигурации, миграции гоняем сами из кода).
 *
 * <p>Changelog лежит в каталоге {@code migrations/} в корне проекта — вне classpath,
 * поэтому используется {@link DirectoryResourceAccessor} с корнем в этом каталоге,
 * а не {@code ClassLoaderResourceAccessor}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiquibaseMigrationRunner {

    private final DataSource dataSource;

    /** Каталог migrations/ относительно рабочей директории (корень проекта). */
    @Value("${app.migrations.directory:migrations}")
    private String migrationsDirectory;

    /** Имя root changelog внутри каталога migrations/. */
    @Value("${app.migrations.changelog:master.xml}")
    private String changelogFile;

    @EventListener(ApplicationReadyEvent.class)
    public void runMigrations() {
        log.info("Running Liquibase migrations from {}/{}", migrationsDirectory, changelogFile);
        try (Connection connection = dataSource.getConnection()) {
            Database database =
                    DatabaseFactory.getInstance()
                            .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Scope.child(
                    Scope.Attr.resourceAccessor,
                    new DirectoryResourceAccessor(new java.io.File(migrationsDirectory)),
                    () -> {
                        new CommandScope("update")
                                .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                                .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, changelogFile)
                                .execute();
                    });

            log.info("Liquibase migrations applied successfully");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to apply Liquibase migrations", e);
        }
    }
}