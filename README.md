# MyBatis Query by Example

[![Build Status](https://travis-ci.org/jeffgbutler/mybatis-qbe.svg?branch=master)](https://travis-ci.org/jeffgbutler/mybatis-qbe)
[![Coverage Status](https://coveralls.io/repos/github/jeffgbutler/mybatis-qbe/badge.svg?branch=master)](https://coveralls.io/github/jeffgbutler/mybatis-qbe?branch=master)

## What Is This?
This library provides a framework for creating dynamic where clauses for SQL statements.
The primary goals of the library are:

1. Typesafe - to the extent possible, the library will ensure that condition parameter types match
   the database field types
2. Expressive - where clauses are built in a way that clearly communicates their meaning
   (thanks to Hamcrest for some inspiration)
3. Flexible - clauses can be built using any combination of and, or, and nested conditions
4. Extensible - the library will render where clauses for MyBatis3 or plain JDBC.  It can be extended to
   generate clauses for other frameworks as well.  Custom conditions can be added easily
   if none of the built in conditions are sufficient for your needs. 
5. Small - the library does one thing only and is a very small dependency to add.  It has no transitive
   dependencies.
   
This library grew out of a desire to create a utility that could be used to improve the code
generated by MyBatis generator, but the library can be used on it's own with very little setup required.

## Requirements

The library has no dependencies.  Java 8 is required.

## Show Me an Example
The goal is to enable very expressive dynamic queries in MyBatis.  Here's an example of what's possible:

```java
    @Test
    public void testComplexCondition() {
        SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            AnimalDataMapper mapper = sqlSession.getMapper(AnimalDataMapper.class);
            
            RenderedWhereClause renderedWhereClause = where(id, isIn(1, 5, 7))
                    .or(id, isIn(2, 6, 8), and(animalName, isLike("%bat")))
                    .or(id, isGreaterThan(60))
                    .and(bodyWeight, isBetween(1.0).and(3.0))
                    .render();

            List<AnimalData> animals = mapper.selectByExample(renderedWhereClause);
            assertThat(animals.size(), is(4));
        } finally {
            sqlSession.close();
        }
    }
```

## How Do I Use It?
The following discussion will walk through an example of using the library.  The full source code
for this example is in ```src/test/java/examples/simple``` in this repo.

The database table used in the example is defined as follows:

```sql
create table SimpleTable (
   id int not null,
   first_name varchar(30) not null,
   last_name varchar(30) not null,
   birth_date date not null, 
   occupation varchar(30) null,
   primary key(id)
);
```
 
### First - Define Database Fields
The class ```org.mybatis.qbe.mybatis3.MyBatis3Field``` is used to define fields for use in the where clause.
Typically these should be defined as public static variables in a class or interface.  This will help make the where clause more expressive.  A field definition includes:

1. The Java type
2. The field name
3. The JDBC type
4. (optional) An alias if used in a query that aliases the table
5. (optional) The name of a type handler to use in MyBatis if the default type handler is not desired

For example:

```java
package examples.simple;

import java.sql.JDBCType;
import java.util.Date;

import org.mybatis.qbe.mybatis3.MyBatis3Field;

public interface SimpleTableFields {
    MyBatis3Field<Integer> id = MyBatis3Field.of("id", JDBCType.INTEGER).withAlias("a");
    MyBatis3Field<String> firstName = MyBatis3Field.of("first_name", JDBCType.VARCHAR).withAlias("a");
    MyBatis3Field<String> lastName = MyBatis3Field.of("last_name", JDBCType.VARCHAR).withAlias("a");
    MyBatis3Field<Date> birthDate = MyBatis3Field.of("birth_date", JDBCType.DATE).withAlias("a");
    MyBatis3Field<String> occupation = MyBatis3Field.of("occupation", JDBCType.VARCHAR).withAlias("a");
}
```

### Second - Write XML or SQL Providers That Will Use the Generated Where Clause
The library will create an object of class ```org.mybatis.qbe.sql.where.render.RenderedWhereClause``` that will be used as input to an SQL provider or an XML mapper.  This object includes the generated where clause, as well as a parameter set that will match the generated clause.  Both are required by MyBatis3.  It is intended that this object be the one and only parameter to a MyBatis method.  Both SQL providers and XML mappers will make use of the rendered where clause.

For example, a SQL provider might look like this:

```java
package examples.simple;

import org.mybatis.qbe.sql.where.render.RenderedWhereClause;

public class SimpleTableSqlProvider {
    
    public String selectByExample(RenderedWhereClause renderedWhereClause) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("select a.id, a.first_name as firstName, a.last_name as lastName, a.birth_date as birthDate, a.occupation ");
        sb.append("from SimpleTable a ");
        sb.append(renderedWhereClause.getWhereClause());
        
        return sb.toString();
    }

    public String deleteByExample(RenderedWhereClause renderedWhereClause) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("delete from SimpleTable ");
        sb.append(renderedWhereClause.getWhereClause());
        
        return sb.toString();
    }
}
```
An XML mapper might look like this:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="examples.simple.SimpleTableMapper">

  <resultMap id="SimpleTableResult" type="examples.simple.SimpleTable">
    <id column="id" jdbcType="INTEGER" property="id" />
    <result column="first_name" jdbcType="VARCHAR" property="firstName" />
    <result column="last_name" jdbcType="VARCHAR" property="lastName" />
    <result column="birth_date" jdbcType="DATE" property="birthDate" />
    <result column="occupation" jdbcType="VARCHAR" property="occupation" />
  </resultMap>

  <select id="selectByExample" resultMap="SimpleTableResult">
    select a.id, a.first_name, a.last_name, a.birth_date, a.occupation
    from SimpleTable a
    ${whereClause}
  </select>

  <delete id="deleteByExample">
    delete from SimpleTable
    ${whereClause}
  </delete>  
</mapper>
```

Notice in both examples that the select uses a table alias and the delete does not.

### Third - Write a Mapper Interface for the Providers and/or XML
This is a typical MyBatis mapper.  The example is as follows:

```java
package examples.simple;

import java.util.List;

import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.SelectProvider;
import org.mybatis.qbe.sql.where.render.RenderedWhereClause;

public interface SimpleTableMapper {
    // methods in XML
    List<SimpleTable> selectByExample(RenderedWhereClause renderedWhereClause);
    int deleteByExample(RenderedWhereClause renderedWhereClause);
    
    // methods in select providers
    @SelectProvider(type=SimpleTableSqlProvider.class, method="selectByExample")
    List<SimpleTable> selectByExampleWithProvider(RenderedWhereClause renderedWhereClause);

    @DeleteProvider(type=SimpleTableSqlProvider.class, method="deleteByExample")
    int deleteByExampleWithProvider(RenderedWhereClause renderedWhereClause);
}
```
### Fourth - Create Where Clauses for your Queries
Where clauses are created by combining your field definition (from the first step above) with a condition for the field.  This library includes a large number of type safe conditions.
All conditions can be accessed through expressive static methods in the ```org.mybatis.qbe.sql.where.SqlConditions``` interface.

For example, a very simple condition can be defined like this:

```java
        RenderedWhereClause renderedWhereClause = where(id, isEqualTo(3))
                .render();
```

Or this:

```java
        RenderedWhereClause renderedWhereClause = where(id, isNull())
                .render();
```

The "between" condition is also expressive:

```java
        RenderedWhereClause renderedWhereClause = where(id, isBetween(1).and(4))
                .render();
```

More complex expressions can be built using the "and" and "or" conditions as follows:

```java
        RenderedWhereClause renderedWhereClause = where(id, isGreaterThan(2))
                .or(occupation, isNull(), and(id, isLessThan(6)))
                .renderIgnoringAlias();
```

Notice that this last where clause will be built without the table alias.  This is useful for some databases that
do not allow table aliases in delete statements.

All of these statements rely on a set of expressive static methods.  It is typical to import the following:

```java
// import all conditions and the where clause builder
import static org.mybatis.qbe.sql.where.SqlConditions.*;
import static org.mybatis.qbe.sql.where.render.WhereClauseShortcut.where;

// import all field definitions for your table
import static examples.simple.SimpleTableFields.*;
```

### Fifth - Use Your Where Clauses
In a DAO or service class, you can use the generated where clause as input to your mapper methods.  Here's
an example from ```examples.simple.SimpleTableTest```:

```java
    @Test
    public void testSelectByExampleInXML() {
        SqlSession session = sqlSessionFactory.openSession();
        try {
            SimpleTableMapper mapper = session.getMapper(SimpleTableMapper.class);
            
            RenderedWhereClause renderedWhereClause = where(id, isEqualTo(1))
                    .or(occupation, isNull())
                    .render();
            
            List<SimpleTable> rows = mapper.selectByExample(renderedWhereClause);
            
            assertThat(rows.size(), is(3));
        } finally {
            session.close();
        }
    }
```

That's it!  Let us know what you think.
