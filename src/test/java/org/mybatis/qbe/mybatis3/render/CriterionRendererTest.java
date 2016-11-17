package org.mybatis.qbe.mybatis3.render;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.sql.JDBCType;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mybatis.qbe.Criterion;
import org.mybatis.qbe.condition.IsEqualToCondition;
import org.mybatis.qbe.condition.IsLikeCondition;
import org.mybatis.qbe.field.Field;

public class CriterionRendererTest {

    @Test
    public void testAliasWithIgnore() {
        Field<Integer> field = Field.of("id", JDBCType.INTEGER).withAlias("a");
        IsEqualToCondition<Integer> condition = IsEqualToCondition.of(3);
        Criterion<Integer> criterion = Criterion.of(field, condition);
        AtomicInteger sequence = new AtomicInteger(1);
        CriterionRendererWithoutTableAlias<Integer> renderer = CriterionRendererWithoutTableAlias.of(criterion, sequence);
        
        RenderedCriterion rc = renderer.render();
        assertThat(rc.whereClauseFragment(), is(" id = #{parameters.p1,jdbcType=INTEGER}"));
        assertThat(rc.fragmentParameters().size(), is(1));
    }

    @Test
    public void testAliasWithoutIgnore() {
        Field<Integer> field = Field.of("id", JDBCType.INTEGER).withAlias("a");
        IsEqualToCondition<Integer> condition = IsEqualToCondition.of(3);
        Criterion<Integer> criterion = Criterion.of(field, condition);
        AtomicInteger sequence = new AtomicInteger(1);
        CriterionRenderer<Integer> renderer = CriterionRenderer.of(criterion, sequence);
        
        RenderedCriterion rc = renderer.render();
        assertThat(rc.whereClauseFragment(), is(" a.id = #{parameters.p1,jdbcType=INTEGER}"));
        assertThat(rc.fragmentParameters().size(), is(1));
    }

    @Test
    public void testNoAliasWithIgnore() {
        Field<Integer> field = Field.of("id", JDBCType.INTEGER);
        IsEqualToCondition<Integer> condition = IsEqualToCondition.of(3);
        Criterion<Integer> criterion = Criterion.of(field, condition);
        AtomicInteger sequence = new AtomicInteger(1);
        CriterionRendererWithoutTableAlias<Integer> renderer = CriterionRendererWithoutTableAlias.of(criterion, sequence);
        
        RenderedCriterion rc = renderer.render();
        assertThat(rc.whereClauseFragment(), is(" id = #{parameters.p1,jdbcType=INTEGER}"));
        assertThat(rc.fragmentParameters().size(), is(1));
    }

    @Test
    public void testNoAliasWithoutIgnore() {
        Field<Integer> field = Field.of("id", JDBCType.INTEGER);
        IsEqualToCondition<Integer> condition = IsEqualToCondition.of(3);
        Criterion<Integer> criterion = Criterion.of(field, condition);
        AtomicInteger sequence = new AtomicInteger(1);
        CriterionRenderer<Integer> renderer = CriterionRenderer.of(criterion, sequence);
        
        RenderedCriterion rc = renderer.render();
        assertThat(rc.whereClauseFragment(), is(" id = #{parameters.p1,jdbcType=INTEGER}"));
        assertThat(rc.fragmentParameters().size(), is(1));
    }

    @Test
    public void testTypeHandler() {
        Field<Date> field = Field.of("id", JDBCType.DATE).withTypeHandler("foo.Bar");
        IsEqualToCondition<Date> condition = IsEqualToCondition.of(new Date());
        Criterion<Date> criterion = Criterion.of(field, condition);
        AtomicInteger sequence = new AtomicInteger(1);
        CriterionRenderer<Date> renderer = CriterionRenderer.of(criterion, sequence);
        
        RenderedCriterion rc = renderer.render();
        assertThat(rc.whereClauseFragment(), is(" id = #{parameters.p1,jdbcType=DATE,typeHandler=foo.Bar}"));
        assertThat(rc.fragmentParameters().size(), is(1));
    }

    @Test
    public void testTypeHandlerAndAlias() {
        Field<Integer> field = Field.of("id", JDBCType.INTEGER).withTypeHandler("foo.Bar").withAlias("a");
        IsEqualToCondition<Integer> condition = IsEqualToCondition.of(3);
        Criterion<Integer> criterion = Criterion.of(field, condition);
        AtomicInteger sequence = new AtomicInteger(1);
        CriterionRenderer<Integer> renderer = CriterionRenderer.of(criterion, sequence);
        
        RenderedCriterion rc = renderer.render();
        assertThat(rc.whereClauseFragment(), is(" a.id = #{parameters.p1,jdbcType=INTEGER,typeHandler=foo.Bar}"));
        assertThat(rc.fragmentParameters().size(), is(1));
    }
    
    @Test
    public void testCustomCondition() {
        Field<String> field = Field.of("description", JDBCType.VARCHAR)
                .withTypeHandler("foo.Bar")
                .withAlias("a");
        
        IsLikeCondition condition = IsLikeCaseInsensitiveCondition.of("fred");
        Criterion<String> criterion = Criterion.of(field, condition);
        AtomicInteger sequence = new AtomicInteger(1);
        CriterionRenderer<String> renderer = CriterionRenderer.of(criterion, sequence);
        
        RenderedCriterion rc = renderer.render();
        assertThat(rc.whereClauseFragment(), is(" upper(a.description) like #{parameters.p1,jdbcType=VARCHAR,typeHandler=foo.Bar}"));
        assertThat(rc.fragmentParameters().size(), is(1));
        assertThat(rc.fragmentParameters().get("p1"), is("FRED"));
    }
    
    public static class IsLikeCaseInsensitiveCondition extends IsLikeCondition {
        private IsLikeCaseInsensitiveCondition(String value) {
            super(value);
        }
        
        public static IsLikeCaseInsensitiveCondition of(String value) {
            return new IsLikeCaseInsensitiveCondition(value);
        }
        
        @Override
        public String fieldName(Field<String> field) {
            return String.format("upper(%s)", field.aliasedName());
        }

        @Override
        public String fieldNameWithoutAlias(Field<String> field) {
            return String.format("upper(%s)", field.name());
        }
        
        @Override
        public String value() {
            return super.value().toUpperCase();
        }
    }
}
