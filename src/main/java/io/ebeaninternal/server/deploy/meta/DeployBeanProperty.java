package io.ebeaninternal.server.deploy.meta;

import io.ebean.annotation.CreatedTimestamp;
import io.ebean.annotation.DocCode;
import io.ebean.annotation.DocProperty;
import io.ebean.annotation.DocSortable;
import io.ebean.annotation.SoftDelete;
import io.ebean.annotation.UpdatedTimestamp;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import io.ebean.annotation.WhoCreated;
import io.ebean.annotation.WhoModified;
import io.ebean.config.ScalarTypeConverter;
import io.ebean.config.dbplatform.DbDefaultValue;
import io.ebean.config.dbplatform.DbEncrypt;
import io.ebean.config.dbplatform.DbEncryptFunction;
import io.ebeaninternal.server.core.InternString;
import io.ebeaninternal.server.deploy.BeanProperty;
import io.ebeaninternal.server.deploy.DeployDocPropertyOptions;
import io.ebeaninternal.server.deploy.DbMigrationInfo;
import io.ebeaninternal.server.deploy.generatedproperty.GeneratedProperty;
import io.ebeaninternal.server.deploy.parse.AnnotationBase;
import io.ebeaninternal.server.el.ElPropertyValue;
import io.ebeaninternal.server.properties.BeanPropertyGetter;
import io.ebeaninternal.server.properties.BeanPropertySetter;
import io.ebeaninternal.server.type.ScalarType;
import io.ebeaninternal.server.type.ScalarTypeWrapper;
import io.ebeanservice.docstore.api.mapping.DocPropertyOptions;

import javax.persistence.EmbeddedId;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Version;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Description of a property of a bean. Includes its deployment information such
 * as database column mapping information.
 */
public class DeployBeanProperty {

  private static final int ID_ORDER = 1000000;
  private static final int UNIDIRECTIONAL_ORDER = 100000;
  private static final int AUDITCOLUMN_ORDER = -1000000;
  private static final int VERSIONCOLUMN_ORDER = -1000000;
  private static final Set<Class<?>> PRIMITIVE_NUMBER_TYPES = new HashSet<>();
  static {
    PRIMITIVE_NUMBER_TYPES.add(float.class);
    PRIMITIVE_NUMBER_TYPES.add(double.class);
    PRIMITIVE_NUMBER_TYPES.add(long.class);
    PRIMITIVE_NUMBER_TYPES.add(int.class);
    PRIMITIVE_NUMBER_TYPES.add(short.class);
  }

  /**
   * Flag to mark this at part of the unique id.
   */
  private boolean id;

  /**
   * Flag to mark the property as embedded. This could be on
   * BeanPropertyAssocOne rather than here. Put it here for checking Id type
   * (embedded or not).
   */
  private boolean embedded;

  /**
   * Flag indicating if this the version property.
   */
  private boolean versionColumn;

  private boolean fetchEager = true;

  /**
   * Set if this property is nullable.
   */
  private boolean nullable = true;

  private boolean unique;

  private boolean discriminator;

  /**
   * The length or precision of the DB column.
   */
  private int dbLength;

  private int dbScale;

  private String dbColumnDefn;

  private boolean isTransient;

  private boolean localEncrypted;

  private boolean jsonSerialize = true;
  private boolean jsonDeserialize = true;

  private boolean dbEncrypted;
  private DbEncryptFunction dbEncryptFunction;

  private int dbEncryptedType;

  private String dbBind = "?";

  /**
   * Is this property include in database resultSet.
   */
  private boolean dbRead;

  /**
   * Include this in DB insert.
   */
  private boolean dbInsertable;

  /**
   * Include this in a DB update.
   */
  private boolean dbUpdateable;

  private DeployTableJoin secondaryTableJoin;

  private String secondaryTableJoinPrefix;

  /**
   * Set to true if this property is based on a secondary table.
   */
  private String secondaryTable;

  /**
   * The type that owns this property.
   */
  private Class<?> owningType;

  /**
   * True if the property is a Clob, Blob LongVarchar or LongVarbinary.
   */
  private boolean lob;

  private boolean naturalKey;

  /**
   * The logical bean property name.
   */
  private String name;

  /**
   * The reflected field.
   */
  private Field field;

  /**
   * The bean type.
   */
  private final Class<?> propertyType;

  private final Type genericType;

  /**
   * Set for Non-JDBC types to provide logical to db type conversion.
   */
  private ScalarType<?> scalarType;

  /**
   * The database column. This can include quoted identifiers.
   */
  private String dbColumn;

  private String aggregationPrefix;
  private String aggregation;

  private String sqlFormulaSelect;
  private String sqlFormulaJoin;

  /**
   * The jdbc data type this maps to.
   */
  private int dbType;

  private DeployDocPropertyOptions docMapping = new DeployDocPropertyOptions();

  /**
   * The method used to read the property.
   */
  private Method readMethod;

  private int propertyIndex;

  private BeanPropertyGetter getter;

  private BeanPropertySetter setter;

  /**
   * Generator for insert or update timestamp etc.
   */
  private GeneratedProperty generatedProperty;

  protected final DeployBeanDescriptor<?> desc;

  private boolean undirectionalShadow;

  private int sortOrder;

  private boolean excludedFromHistory;

  private boolean tenantId;

  private boolean draft;
  private boolean draftOnly;
  private boolean draftDirty;
  private boolean draftReset;

  private boolean softDelete;
  private boolean unmappedJson;

  private String dbComment;

  private String dbColumnDefault;

  private List<DbMigrationInfo> dbMigrationInfos;

  public DeployBeanProperty(DeployBeanDescriptor<?> desc, Class<?> propertyType, ScalarType<?> scalarType, ScalarTypeConverter<?, ?> typeConverter) {
    this.desc = desc;
    this.propertyType = propertyType;
    this.genericType = null;
    this.scalarType = wrapScalarType(propertyType, scalarType, typeConverter);
    this.dbType = (scalarType == null) ? 0 : scalarType.getJdbcType();
  }

  public DeployBeanProperty(DeployBeanDescriptor<?> desc, Class<?> propertyType, Type genericType) {
    this.desc = desc;
    this.propertyType = propertyType;
    this.genericType = genericType;
  }

  /**
   * Wrap the ScalarType using a ScalarTypeConverter.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private ScalarType<?> wrapScalarType(Class<?> propertyType, ScalarType<?> scalarType, ScalarTypeConverter<?, ?> typeConverter) {
    if (typeConverter == null) {
      return scalarType;
    }
    return new ScalarTypeWrapper(propertyType, scalarType, typeConverter);
  }

  public int getSortOverride() {
    if (field == null) {
      return 0;
    }
    if (AnnotationBase.findAnnotation(field, Id.class) != null) {
      return ID_ORDER;
    } else if (AnnotationBase.findAnnotation(field, EmbeddedId.class) != null) {
      return ID_ORDER;
    } else if (undirectionalShadow) {
      return UNIDIRECTIONAL_ORDER;
    } else if (isAuditProperty()) {
      return AUDITCOLUMN_ORDER;
    } else if (AnnotationBase.findAnnotation(field, Version.class) != null) {
      return VERSIONCOLUMN_ORDER;
    } else if (AnnotationBase.findAnnotation(field, SoftDelete.class) != null) {
      return VERSIONCOLUMN_ORDER;
    }
    return 0;
  }

  private boolean isAuditProperty() {
    return (AnnotationBase.findAnnotation(field, WhenCreated.class) != null
      || AnnotationBase.findAnnotation(field, WhenModified.class) != null
      || AnnotationBase.findAnnotation(field, WhoModified.class) != null
      || AnnotationBase.findAnnotation(field, WhoCreated.class) != null
      || AnnotationBase.findAnnotation(field, UpdatedTimestamp.class) != null
      || AnnotationBase.findAnnotation(field, CreatedTimestamp.class) != null);
  }

  public String getFullBeanName() {
    return desc.getFullName() + "." + name;
  }

  /**
   * Return the DB column length for character columns.
   * <p>
   * Note if there is no length explicitly defined then the scalarType is
   * checked to see if that has one (primarily to support putting a length on
   * Enum types).
   * </p>
   */
  public int getDbLength() {
    if (dbLength == 0 && scalarType != null) {
      return scalarType.getLength();
    }

    return dbLength;
  }

  public boolean isJsonSerialize() {
    return jsonSerialize;
  }

  public void setJsonSerialize(boolean jsonSerialize) {
    this.jsonSerialize = jsonSerialize;
  }

  public boolean isJsonDeserialize() {
    return jsonDeserialize;
  }

  public void setJsonDeserialize(boolean jsonDeserialize) {
    this.jsonDeserialize = jsonDeserialize;
  }

  /**
   * Return the sortOrder for the properties.
   */
  public int getSortOrder() {
    return sortOrder;
  }

  /**
   * Set the sortOrder for the properties.
   */
  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  /**
   * Return true if this is a placeholder property for a unidirectional relationship.
   */
  public boolean isUndirectionalShadow() {
    return undirectionalShadow;
  }

  /**
   * Mark this property as a placeholder for a unidirectional relationship.
   */
  public void setUndirectionalShadow() {
    this.undirectionalShadow = true;
  }

  /**
   * Mark this property as mapping to the discriminator column.
   */
  public void setDiscriminator() {
    this.discriminator = true;
  }

  /**
   * Return true if this property maps to the inheritance discriminator column.s
   */
  public boolean isDiscriminator() {
    return discriminator;
  }

  /**
   * Return true if the property is encrypted in java rather than in the DB.
   */
  public boolean isLocalEncrypted() {
    return localEncrypted;
  }

  /**
   * Set to true when the property is encrypted in java rather than in the DB.
   */
  public void setLocalEncrypted() {
    this.localEncrypted = true;
  }

  /**
   * Set the DB column length for character columns.
   */
  public void setDbLength(int dbLength) {
    this.dbLength = dbLength;
  }

  /**
   * Return the Db scale for numeric columns.
   */
  public int getDbScale() {
    return dbScale;
  }

  /**
   * Set the Db scale for numeric columns.
   */
  public void setDbScale(int dbScale) {
    this.dbScale = dbScale;
  }

  /**
   * Return the DB column definition if defined.
   */
  public String getDbColumnDefn() {
    return dbColumnDefn;
  }

  /**
   * Set a specific DB column definition.
   */
  public void setDbColumnDefn(String dbColumnDefn) {
    if (dbColumnDefn == null || dbColumnDefn.trim().isEmpty()) {
      this.dbColumnDefn = null;
    } else {
      this.dbColumnDefn = InternString.intern(dbColumnDefn);
    }
  }

  /**
   * Return the scalarType. This returns null for native JDBC types, otherwise
   * it is used to convert between logical types and jdbc types.
   */
  public ScalarType<?> getScalarType() {
    return scalarType;
  }

  public void setScalarType(ScalarType<?> scalarType) {
    this.scalarType = scalarType;
  }

  public int getPropertyIndex() {
    return propertyIndex;
  }

  public void setPropertyIndex(int propertyIndex) {
    this.propertyIndex = propertyIndex;
  }

  public BeanPropertyGetter getGetter() {
    return getter;
  }

  public BeanPropertySetter getSetter() {
    return setter;
  }

  /**
   * Return the getter method.
   */
  public Method getReadMethod() {
    return readMethod;
  }

  /**
   * Set to the owning type form a Inheritance heirarchy.
   */
  public void setOwningType(Class<?> owningType) {
    this.owningType = owningType;
  }

  public Class<?> getOwningType() {
    return owningType;
  }

  /**
   * Return true if this is local to this type - aka not from a super type.
   */
  public boolean isLocal() {
    return owningType == null || owningType.equals(desc.getBeanType());
  }

  /**
   * Set the getter used to read the property value from a bean.
   */
  public void setGetter(BeanPropertyGetter getter) {
    this.getter = getter;
  }

  /**
   * Set the setter used to set the property value to a bean.
   */
  public void setSetter(BeanPropertySetter setter) {
    this.setter = setter;
  }

  /**
   * Return the name of the property.
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the property.
   */
  public void setName(String name) {
    this.name = InternString.intern(name);
  }

  /**
   * Return the bean Field associated with this property.
   */
  public Field getField() {
    return field;
  }

  /**
   * Set the bean Field associated with this property.
   */
  public void setField(Field field) {
    this.field = field;
  }

  public boolean isNaturalKey() {
    return naturalKey;
  }

  public void setNaturalKey() {
    this.naturalKey = true;
  }

  /**
   * Return the GeneratedValue. Used to generate update timestamp etc.
   */
  public GeneratedProperty getGeneratedProperty() {
    return generatedProperty;
  }

  /**
   * Set the GeneratedValue. Used to generate update timestamp etc.
   */
  public void setGeneratedProperty(GeneratedProperty generatedValue) {
    this.generatedProperty = generatedValue;
  }

  /**
   * Return true if this property is mandatory.
   */
  public boolean isNullable() {
    return nullable;
  }

  /**
   * Set the not nullable of this property.
   */
  public void setNullable(boolean isNullable) {
    this.nullable = isNullable;
  }

  /**
   * Return true if the DB column is unique.
   */
  public boolean isUnique() {
    return unique;
  }

  /**
   * Set to true if the DB column is unique.
   */
  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  /**
   * Return true if this is a version column used for concurrency checking.
   */
  public boolean isVersionColumn() {
    return versionColumn;
  }

  /**
   * Set if this is a version column used for concurrency checking.
   */
  public void setVersionColumn() {
    this.versionColumn = true;
  }

  /**
   * Return true if this should be eager fetched by default.
   */
  public boolean isFetchEager() {
    return fetchEager;
  }

  /**
   * Set the default fetch type for this property.
   */
  public void setFetchType(FetchType fetchType) {
    this.fetchEager = FetchType.EAGER.equals(fetchType);
  }

  /**
   * Return the formula this property is based on.
   */
  public String getSqlFormulaSelect() {
    return sqlFormulaSelect;
  }

  public String getSqlFormulaJoin() {
    return sqlFormulaJoin;
  }

  /**
   * The property is based on a formula.
   */
  public void setSqlFormula(String formulaSelect, String formulaJoin) {
    this.sqlFormulaSelect = formulaSelect;
    this.sqlFormulaJoin = formulaJoin.isEmpty() ? null : formulaJoin;
    this.dbRead = true;
    this.dbInsertable = false;
    this.dbUpdateable = false;
  }


  public boolean isAggregation() {
    return aggregation != null;
  }

  public String getAggregation() {
    return aggregation;
  }

  public void setAggregation(String aggregation) {
    this.aggregation = aggregation;
    this.dbRead = true;
    this.dbInsertable = false;
    this.dbUpdateable = false;
  }

  /**
   * Set the path to the aggregation.
   */
  public void setAggregationPrefix(String aggregationPrefix) {
    this.aggregationPrefix = aggregationPrefix;
    this.aggregation = aggregation.replace(aggregationPrefix, "u1");
  }

  public String getElPrefix() {
    if (aggregation != null) {
      return aggregationPrefix;
    } else {
      return secondaryTableJoinPrefix;
    }
  }

  public String getElPlaceHolder() {
    if (aggregation != null) {
      return aggregation;
    } else if (sqlFormulaSelect != null) {
      return sqlFormulaSelect;
    } else {
      if (secondaryTableJoinPrefix != null) {
        return "${" + secondaryTableJoinPrefix + "}" + getDbColumn();
      }
      // prepend table alias placeholder
      return ElPropertyValue.ROOT_ELPREFIX + getDbColumn();
    }
  }

  /**
   * The database column name this is mapped to.
   */
  public String getDbColumn() {
    if (sqlFormulaSelect != null) {
      return sqlFormulaSelect;
    }
    if (aggregation != null) {
      return aggregation;
    }
    return dbColumn;
  }

  /**
   * Set the database column name this is mapped to.
   */
  public void setDbColumn(String dbColumn) {
    this.dbColumn = InternString.intern(dbColumn);
  }

  /**
   * Return the database jdbc data type this is mapped to.
   */
  public int getDbType() {
    return dbType;
  }

  /**
   * Set the database jdbc data type this is mapped to.
   */
  public void setDbType(int dbType) {
    this.dbType = dbType;
    this.lob = BeanProperty.isLobType(dbType);
  }

  /**
   * Return true if this is mapped to a Clob Blob LongVarchar or
   * LongVarbinary.
   */
  public boolean isLob() {
    return lob;
  }

  public boolean isDbNumberType() {
    return isNumericType(dbType);
  }

  private boolean isNumericType(int type) {
    switch (type) {
      case Types.BIGINT:
        return true;
      case Types.DECIMAL:
        return true;
      case Types.DOUBLE:
        return true;
      case Types.FLOAT:
        return true;
      case Types.INTEGER:
        return true;
      case Types.NUMERIC:
        return true;
      case Types.REAL:
        return true;
      case Types.SMALLINT:
        return true;
      case Types.TINYINT:
        return true;

      default:
        return false;
    }
  }

  /**
   * Return true if this property is based on a secondary table.
   */
  public boolean isSecondaryTable() {
    return secondaryTable != null;
  }

  /**
   * Return the secondary table this property is associated with.
   */
  public String getSecondaryTable() {
    return secondaryTable;
  }

  /**
   * Set to true if this property is included in persisting.
   */
  public void setSecondaryTable(String secondaryTable) {
    this.secondaryTable = secondaryTable;
    this.dbInsertable = false;
    this.dbUpdateable = false;
  }

  /**
   *
   */
  public String getSecondaryTableJoinPrefix() {
    return secondaryTableJoinPrefix;
  }

  public DeployTableJoin getSecondaryTableJoin() {
    return secondaryTableJoin;
  }

  public void setSecondaryTableJoin(DeployTableJoin secondaryTableJoin, String prefix) {
    this.secondaryTableJoin = secondaryTableJoin;
    this.secondaryTableJoinPrefix = prefix;
  }

  /**
   * Return the DB Bind parameter. Typically is "?" but can be different for
   * encrypted bind.
   */
  public String getDbBind() {
    return dbBind;
  }

  /**
   * Return true if this property is encrypted in the DB.
   */
  public boolean isDbEncrypted() {
    return dbEncrypted;
  }

  public DbEncryptFunction getDbEncryptFunction() {
    return dbEncryptFunction;
  }

  public void setDbEncryptFunction(DbEncryptFunction dbEncryptFunction, DbEncrypt dbEncrypt, int dbLen) {
    this.dbEncryptFunction = dbEncryptFunction;
    this.dbEncrypted = true;
    this.dbBind = dbEncryptFunction.getEncryptBindSql();

    this.dbEncryptedType = isLob() ? Types.BLOB : dbEncrypt.getEncryptDbType();
    if (dbLen > 0) {
      setDbLength(dbLen);
    }
  }

  /**
   * Return the DB type for the encrypted property. This can differ from the
   * logical type (String encrypted and stored in a VARBINARY)
   */
  public int getDbEncryptedType() {
    return dbEncryptedType;
  }

  /**
   * Return true if this property is included in database queries.
   */
  public boolean isDbRead() {
    return dbRead;
  }

  /**
   * Set to true if this property is included in database queries.
   */
  public void setDbRead(boolean isDBRead) {
    this.dbRead = isDBRead;
  }

  public boolean isDbInsertable() {
    return dbInsertable;
  }

  public void setDbInsertable(boolean insertable) {
    this.dbInsertable = insertable;
  }

  public boolean isDbUpdateable() {
    return dbUpdateable;
  }

  public void setDbUpdateable(boolean updateable) {
    this.dbUpdateable = updateable;
  }

  /**
   * Return true if the property is transient.
   */
  public boolean isTransient() {
    return isTransient;
  }

  /**
   * Mark the property explicitly as a transient property.
   */
  public void setTransient() {
    this.isTransient = true;
  }

  /**
   * Set the bean read method.
   * <p>
   * NB: That a BeanReflectGetter is used to actually perform the getting of
   * property values from a bean. This is due to performance considerations.
   * </p>
   */
  public void setReadMethod(Method readMethod) {
    this.readMethod = readMethod;
  }

  /**
   * Return the property type.
   */
  public Class<?> getPropertyType() {
    return propertyType;
  }

  /**
   * Return the generic type for this property.
   */
  public Type getGenericType() {
    return genericType;
  }

  /**
   * Return true if this is included in the unique id.
   */
  public boolean isId() {
    return id;
  }

  /**
   * Set to true if this is included in the unique id.
   */
  public void setId() {
    this.id = true;
  }

  /**
   * Return true if this is an Embedded property. In this case it shares the
   * table and pk of its owner object.
   */
  public boolean isEmbedded() {
    return embedded;
  }

  /**
   * Set to true if this is an embedded property.
   */
  public void setEmbedded() {
    this.embedded = true;
  }

  @Override
  public String toString() {
    return desc.getFullName() + "." + name;
  }

  public boolean isExcludedFromHistory() {
    return excludedFromHistory;
  }

  public void setExcludedFromHistory() {
    this.excludedFromHistory = true;
  }

  public void setDraft() {
    this.draft = true;
    this.isTransient = true;
  }

  public boolean isDraft() {
    return draft;
  }

  public void setDraftOnly() {
    this.draftOnly = true;
  }

  public boolean isDraftOnly() {
    return draftOnly;
  }

  public void setDraftDirty() {
    this.draftOnly = true;
    this.draftDirty = true;
    this.nullable = false;
  }

  public boolean isDraftDirty() {
    return draftDirty;
  }

  public void setDraftReset() {
    this.draftReset = true;
  }

  public boolean isDraftReset() {
    return draftReset;
  }

  /**
   * Primitive boolean check so see if not null default false should be applied.
   */
  public void checkPrimitiveBoolean() {
    if (boolean.class.equals(propertyType) && !softDelete) {
      this.nullable = false;
      if (dbColumnDefault == null) {
        this.dbColumnDefault = DbDefaultValue.FALSE;
      }

    } else if (!id && !versionColumn && PRIMITIVE_NUMBER_TYPES.contains(propertyType)) {
      this.nullable = false;
    }
  }

  public void setSoftDelete() {
    this.softDelete = true;
    this.nullable = false;
    this.dbColumnDefault = DbDefaultValue.FALSE;
  }

  public boolean isSoftDelete() {
    return softDelete;
  }

  public void setUnmappedJson() {
    this.unmappedJson = true;
    this.isTransient = true;
  }

  public boolean isUnmappedJson() {
    return unmappedJson;
  }

  public void setDbComment(String dbComment) {
    this.dbComment = dbComment;
  }

  public String getDbComment() {
    return dbComment;
  }

  public void setDocProperty(DocProperty docProperty) {
    docMapping.setDocProperty(docProperty);
  }

  public void setDocSortable(DocSortable docSortable) {
    docMapping.setDocSortable(docSortable);
  }

  public void setDocCode(DocCode docCode) {
    docMapping.setDocCode(docCode);
  }

  public DocPropertyOptions getDocPropertyOptions() {
    return docMapping.create();
  }

  /**
   * Return the DB Column default taking into account literal translation.
   */
  public String getDbColumnDefaultSqlLiteral() {
    return DbDefaultValue.toSqlLiteral(dbColumnDefault, propertyType, dbType);
  }

  public void setDbColumnDefault(String dbColumnDefault) {
    this.dbColumnDefault = dbColumnDefault;
  }

  public void setTenantId() {
    this.tenantId = true;
    this.nullable = false;
    this.dbInsertable = true;
    this.dbUpdateable = false;
  }

  public boolean isTenantId() {
    return tenantId;
  }

  public void addDbMigrationInfo(DbMigrationInfo info) {
    if (dbMigrationInfos == null) {
      dbMigrationInfos = new ArrayList<>();
    }
    dbMigrationInfos.add(info);
  }

  public List<DbMigrationInfo> getDbMigrationInfos() {
    return dbMigrationInfos;
  }

}
