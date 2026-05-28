package com.woorifisan.bank.domain.document.mapper;

import com.woorifisan.bank.domain.document.model.CommonDocument;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommonDocumentMapper {
    Optional<CommonDocument> findById(Long id);
    void insertDocument(CommonDocument document);
}
