package hello.itemservice.repository.jdbctemplate;

import hello.itemservice.domain.Item;
import hello.itemservice.repository.ItemRepository;
import hello.itemservice.repository.ItemSearchCondition;
import hello.itemservice.repository.ItemUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * JdbcTemplate
 */
@Slf4j
@Repository
public class JdbcTemplateItemRepositoryV1 implements ItemRepository {

    private final JdbcTemplate template;

    public JdbcTemplateItemRepositoryV1(DataSource dataSource) {
        this.template = new JdbcTemplate(dataSource);
    }

    @Override
    public Item save(Item item) {
        String sql = "insert into item (item_name, price, quantity) values(?, ?, ?)";

        // identity 전략 사용 -> 저장 SQL 쿼리 실행 후 ID를 알 수 있음 -> KeyHolder 필요
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // 자동 생성(증가) 키
        template.update(connection -> {
            PreparedStatement pstmt = connection.prepareStatement(sql, new String[]{"id"});

            pstmt.setString(1, item.getItemName());
            pstmt.setInt(2, item.getPrice());
            pstmt.setInt(3, item.getQuantity());

            return pstmt;
        }, keyHolder);

        long id = keyHolder.getKey().longValue();
        item.setId(id);

        return item;
    }

    @Override
    public void update(Long itemId, ItemUpdateDto updateParam) {
        String sql = "update item set item_name=?, price=?, quantity=? where id=?";

        template.update(sql, updateParam.getItemName(), updateParam.getPrice(), updateParam.getQuantity(), itemId);
    }

    @Override
    public Optional<Item> findById(Long id) {
        String sql = "select id, item_name, price, quantity from item where id=?";

        try {
            Item item = template.queryForObject(sql, itemRowMapper(), id);
            return Optional.of(item);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Item> findAll(ItemSearchCondition condition) {
        String sql = "select id, item_name, price, quantity from item";

        String itemName = condition.getItemName();
        Integer maxPrice = condition.getMaxPrice();

        //동적 쿼리 추가
        //검색 조건에 상품명 OR 가격이 있으면 sql문에 "where" 추가
        if (StringUtils.hasText(itemName) || maxPrice != null) {
            sql += " where";
        }

        boolean andFlag = false;
        //sql 실행할 때 넘길 파라미터를 담은 리스트
        List<Object> param = new ArrayList<>();

        //검색 조건에 상품명이 있을 시, sql문에 추가
        if (StringUtils.hasText(itemName)) {
            sql += " item_name like concat('%', ?, '%')";
            param.add(itemName);
            andFlag = true;
        }

        //검새 조건에 가격이 있을 시, sql문에 추가
        if (maxPrice != null) {

            // andFlag == true면 상품명 조건이 있다는 뜻 -> " where"문에 " and" 추가 -> 가격 조건도 걸어줘야 하니까
            if (andFlag) {
                sql += " and";
            }

            sql += " price <= ?";
            param.add(maxPrice);
        }

        log.info("sql={}", sql);
        //결과 값이 1개 이상일 때 -> template.query()
        return template.query(sql, itemRowMapper(), param.toArray());
    }

    /**
     * RowMapper -> resultSet을 객체로 변환
     *
     * @return item
     */
    private RowMapper<Item> itemRowMapper() {
        return (rs, rowNum) -> {
            Item item = new Item();
            item.setId(rs.getLong("id"));
            item.setItemName(rs.getString("item_name"));
            item.setPrice(rs.getInt("price"));
            item.setQuantity(rs.getInt("quantity"));
            return item;
        };
    }
}
