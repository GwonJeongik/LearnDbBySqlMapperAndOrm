package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCondition;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * NamedParameterJdbcTemplate
 * SqlParameterSource
 * - BeanPropertySqlParameterSource
 * - MapSqlParameterSource
 * Map
 * <p>
 * BeanPropertyRowMapper
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV2 implements ItemRepository {

    private final NamedParameterJdbcTemplate template;

    public JdbcTemplateItemRepositoryV2(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Item save(Item item) {
        String sql = """
                insert into item (item_name, price, quantity) values(:itemName, :price, :quantity)
                """;


        SqlParameterSource param = new BeanPropertySqlParameterSource(item);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        template.update(sql, param, keyHolder);

        long id = keyHolder.getKey().longValue();
        item.setId(id);

        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = """
                update item set item_name=:itemName, price=:price, quantity=:quantity
                where id=:id
                """;

        MapSqlParameterSource param = new MapSqlParameterSource()
                .addValue("itemName", updateParam.getItemName())
                .addValue("price", updateParam.getPrice())
                .addValue("quantity", updateParam.getQuantity())
                // BeanPropertySqlParameterSource를 사용할 수 없는 부분 -> 별도로 필요
                .addValue("id", itemId);

        template.update(sql, param);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = """
                select id, item_name, price, quantity from item where id=:id
                """;

        try {
            Map<String, Long> param = Map.of("id", id);

            Item item = template.queryForObject(sql, param, itemRowMapper());
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Item> findAll(ItemSearchCondition condition) {
        String sql = """
                select id, item_name, price, quantity from item
                """;

        String itemName = condition.getItemName();
        Integer maxPrice = condition.getMaxPrice();

        BeanPropertySqlParameterSource param = new BeanPropertySqlParameterSource(condition);

        //동적 쿼리 추가
        //검색 조건에 상품명 OR 가격이 있으면 sql문에 "where" 추가
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;

        //검색 조건에 상품명이 있을 시, sql문에 추가
        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%', :itemName, '%')";
            andFlag = true;
        }

        //검새 조건에 가격이 있을 시, sql문에 추가
        if (maxPrice != null) {

            // andFlag == true면 상품명 조건이 있다는 뜻 -> " where"문에 " and" 추가 -> 가격 조건도 걸어줘야 하니까
            if (andFlag) {
                sql += " and";
            }

            sql += " price <= :maxPrice";
        }

        log.info("sql={}", sql);
        //결과 값이 1개 이상일 때 -> template.query()
        return template.query(sql, param, itemRowMapper());
    }

    /**
     * RowMapper -> resultSet을 객체로 변환
     *
     * @return item
     */
    private RowMapper<Item> itemRowMapper() {
        //Camel 표기법으로 변환 지원
        return BeanPropertyRowMapper.newInstance(Item.class);
    }
}
