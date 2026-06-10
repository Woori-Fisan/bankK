package com.woorifisan.platform.domain.rag.repository;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.qdrant.client.ConditionFactory.matchKeyword;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class QdrantRepository {

    private final QdrantClient qdrantClient;
    private final EmbeddingModel embeddingModel;

    private static final String COLLECTION_NAME = "agency-docs";
    private static final int SEARCH_RESULT_LIMIT = 3;

    private final AtomicBoolean collectionExists = new AtomicBoolean(false);

    private final TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
            .withChunkSize(800)
            .withMinChunkSizeChars(100)
            .withKeepSeparator(true)
            .build();

    private void ensureCollectionExists() throws Exception {
        if (collectionExists.get()) return;
        List<String> names = qdrantClient.listCollectionsAsync().get();
        if (!names.contains(COLLECTION_NAME)) {
            qdrantClient.createCollectionAsync(COLLECTION_NAME,
                    VectorParams.newBuilder()
                            .setSize(3072)
                            .setDistance(Distance.Cosine)
                            .build()).get();
            log.info("[QdrantRepository] 컬렉션 '{}' 생성 완료", COLLECTION_NAME);
        }
        collectionExists.set(true);
    }

    public void saveDocuments(String documentId, String text, Map<String, Object> metadata) {
        log.info("[QdrantRepository] 문서 처리 시작. ID: {}", documentId);

        try {
            ensureCollectionExists();
        } catch (Exception e) {
            throw new RuntimeException("Qdrant 컬렉션 초기화 실패", e);
        }

        // 1. Chunking
        Document rootDoc = new Document(text, metadata);
        List<Document> chunks = tokenTextSplitter.apply(List.of(rootDoc));
        log.info("[QdrantRepository] 청킹 완료. 조각 개수: {}", chunks.size());

        // 2. Build Points
        List<PointStruct> points = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            float[] embedding = embeddingModel.embed(chunk);
            
            UUID pointId = UUID.nameUUIDFromBytes((documentId + "-" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            points.add(PointStruct.newBuilder()
                    .setId(id(pointId))
                    .setVectors(vectors(toFloatList(embedding)))
                    .putAllPayload(buildPayload(chunk, documentId, i))
                    .build());
        }

        // 3. Upsert
        try {
            qdrantClient.upsertAsync(COLLECTION_NAME, points).get();
            log.info("[QdrantRepository] {}개의 청크 저장 완료. 컬렉션: {}", points.size(), COLLECTION_NAME);
        } catch (Exception e) {
            log.error("[QdrantRepository] 저장 중 오류 발생", e);
            throw new RuntimeException("Qdrant 저장 실패", e);
        }
    }

    public void deleteByDocumentId(String documentId) {
        Filter filter = Filter.newBuilder()
                .addMust(matchKeyword("documentId", documentId))
                .build();
        try {
            qdrantClient.deleteAsync(COLLECTION_NAME, filter).get();
            log.info("[QdrantRepository] documentId={} 포인트 삭제 완료", documentId);
        } catch (Exception e) {
            log.error("[QdrantRepository] 삭제 중 오류 발생", e);
        }
    }

    public List<String> searchSimilar(String query) {
        float[] queryEmbedding = embeddingModel.embed(query);

        SearchPoints searchRequest = SearchPoints.newBuilder()
                .setCollectionName(COLLECTION_NAME)
                .addAllVector(toFloatList(queryEmbedding))
                .setLimit(SEARCH_RESULT_LIMIT)
                .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                .build();

        try {
            List<ScoredPoint> results = qdrantClient.searchAsync(searchRequest).get();
            return results.stream()
                    .filter(Objects::nonNull)
                    .map(point -> point.getPayloadMap().get("content"))
                    .filter(Objects::nonNull)
                    .map(Value::getStringValue)
                    .toList();
        } catch (Exception e) {
            log.error("[QdrantRepository] 검색 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float f : vector) list.add(f);
        return list;
    }

    private Map<String, Value> buildPayload(Document chunk, String documentId, int index) {
        Map<String, Value> payload = new HashMap<>();
        payload.put("content", value(chunk.getText()));
        payload.put("documentId", value(documentId));
        payload.put("chunkIndex", value(index));

        chunk.getMetadata().forEach((k, v) -> {
            if (v instanceof String s) payload.put(k, value(s));
            else if (v instanceof Number n) payload.put(k, value(n.doubleValue()));
            else if (v instanceof Boolean b) payload.put(k, value(b));
            else if (v != null) payload.put(k, value(v.toString()));
        });

        return payload;
    }
}
