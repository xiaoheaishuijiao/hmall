package com.hmall.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.query.ItemPageQuery;

import java.io.IOException;
import java.util.List;

public interface ISearchService extends IService<Item> {
    PageDTO<ItemDTO> listByES(ItemPageQuery query) throws IOException;

    ItemDTO listByIdByES(Long id) throws IOException;

    List<ItemDTO> listByIdsByES(List<Long> ids) throws IOException;
}
