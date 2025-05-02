package com.userapi.models.external;

import lombok.Data;

@Data
public class Pagination {
    private Integer page = 0;
    private Integer size = 10;
}
