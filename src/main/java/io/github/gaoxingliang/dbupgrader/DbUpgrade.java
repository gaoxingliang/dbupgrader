package io.github.gaoxingliang.dbupgrader;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface DbUpgrade {
    /**
     * this upgrade class apply to which target version
     * any version <= {@link UpgradeConfiguration#getTargetVersion()} will be executed
     * @return
     */
    int ver();

    /**
     * After which dbUprade class name.
     * For example V1AddTable.  then the full class will be:
     * {@link UpgradeConfiguration#getUpgradeConfigurationTable()} + ".V1AddTable"
     * @return
     */
    String after() default "";
}