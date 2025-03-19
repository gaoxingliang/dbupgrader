package io.github.codeed.dbupgrader;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface DbUpgrade {
    /**
     * this upgrade class apply to which target version
     * any version less or equal than {@link UpgradeConfiguration#targetVersion} will be executed
     * @return
     */
    int version();

    /**
     * After which dbUprade class name.
     * For example V1AddTable.  then the full class will be:
     * {@link UpgradeConfiguration#upgradeClassPackage} + ".V1AddTable"
     * @return
     */
    String after() default "";

    /**
     * how many records this upgradeprocess may affect.
     * If the script update records more than this, it will rollback and throw an exception.
     * @return -1 means no limit
     */
    int maxAffectRecords() default 100;
}