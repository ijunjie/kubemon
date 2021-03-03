package com.kingsoft.kubemon.function;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SplitCollections {

    /**
     * 分段
     *
     * @param totalSize   集合总长度
     * @param segmentSize 每一段长度
     * @return 分段数
     */
    private static int segment(int totalSize, int segmentSize) {
        return (totalSize + segmentSize - 1) / segmentSize;
    }

    /**
     * 将一个集合进行分割
     *
     * @param collections 需要分割的集合 (List LinkedHashSet TreeSet LinkedList)
     * @param segmentSize 分段数
     * @param <T>         元素类型
     * @return 分割后的集合
     */
    public static <T> List<List<T>> split(Collection<T> collections, int segmentSize) {
        int segmentCount = segment(collections.size(), segmentSize);
        return Stream.iterate(0, n -> n + 1).limit(segmentCount)
                .map(a -> collections.stream()
                        .skip((long) a * segmentSize)
                        .limit(segmentSize)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

}