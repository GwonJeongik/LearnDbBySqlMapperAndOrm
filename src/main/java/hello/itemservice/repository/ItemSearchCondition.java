package hello.itemservice.repository;

import lombok.Data;

@Data
public class ItemSearchCondition {

    private String itemName;
    private Integer maxPrice;

    public ItemSearchCondition() {
    }

    public ItemSearchCondition(String itemName, Integer maxPrice) {
        this.itemName = itemName;
        this.maxPrice = maxPrice;
    }
}
