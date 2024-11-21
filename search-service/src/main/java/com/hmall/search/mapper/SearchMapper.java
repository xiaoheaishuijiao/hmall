package com.hmall.search.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.search.domain.po.Item;
import com.hmall.search.domain.query.ItemPageQuery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SearchMapper extends BaseMapper<Item> {
}
