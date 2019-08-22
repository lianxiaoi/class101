package cn.itcast;

import cn.itcast.pojo.Item;
import com.google.gson.Gson;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestESManager {

    private RestHighLevelClient client = null;

    @Before
    public void init() {
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("localhost", 9201, "http"),
                        new HttpHost("localhost", 9202, "http"),
                        new HttpHost("localhost", 9203, "http")));
    }

    Gson gson = new Gson();

    //新增/修改
    @Test
    public void addDocument() throws Exception {
        //创建一个item对象
        Item item = new Item(1L,"小米 Mix3 手机","手机","小米",1199.0,"weehtgc");
        //创建用来创建文档的请求对象
        IndexRequest request = new IndexRequest("leyou", "item", item.getId().toString());
        //使用gson把对象转成jsonzifuchuan
        String jsonString = gson.toJson(item);
        request.source(jsonString, XContentType.JSON);
        //是哟用客户端创建
        client.index(request, RequestOptions.DEFAULT);
    }

    //删除文档
    @Test
    public void delDocument() throws Exception {
        //创建用来删除文档的请求对象
        DeleteRequest request = new DeleteRequest("leyou","item","1");
        client.delete(request,RequestOptions.DEFAULT);
    }


    //批量新增文档
    @Test
    public void addDocumentBulk() throws Exception {
        //准备文旦数据
        List<Item> list = new ArrayList<>();
        list.add(new Item(1L, "小米手机7", "手机", "小米", 3299.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Item(2L, "坚果手机R1", "手机", "锤子", 3699.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Item(3L, "华为META10", "手机", "华为", 4499.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Item(4L, "小米Mix2S", "手机", "小米", 4299.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Item(5L, "荣耀V10", "手机", "华为", 2799.00, "http://image.leyou.com/13123.jpg"));
        //创建用来批量创建文档的请求对象
        BulkRequest bulkRequest = new BulkRequest();
        for (Item item : list) {
            bulkRequest.add(new IndexRequest("leyou","item",item.getId().toString()).source(gson.toJson(item),XContentType.JSON));
        }
        //使用客户端创建
        client.bulk(bulkRequest,RequestOptions.DEFAULT);
    }

    //各种查询
    @Test
    public void testQuery() throws IOException {
        //构建大的查询对象  所有查询方式都能使用  可以指定索引库的名称
        SearchRequest searchRequest = new SearchRequest("leyou");
        //用来构建查询方式
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //构建查询方式
//        searchSourceBuilder.query(QueryBuilders.matchAllQuery()); //查询所有
//        searchSourceBuilder.query(QueryBuilders.termQuery("title","小米")); //term查询
//        searchSourceBuilder.query(QueryBuilders.matchQuery("title","小米手机")); //分词查询
//        searchSourceBuilder.query(QueryBuilders.rangeQuery("price").gte(1000).lte(3000)); //区间查询
//        searchSourceBuilder.query(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("title","小米")).must(QueryBuilders.rangeQuery("price").gte(1000).lte(4000))); //组合查询
//        searchSourceBuilder.query(QueryBuilders.fuzzyQuery("title","大米").fuzziness(Fuzziness.ONE)); //容错查询
//        searchSourceBuilder.query(QueryBuilders.wildcardQuery("title","*小*")); //通配符查询

//        searchSourceBuilder.query(QueryBuilders.matchAllQuery()); //查询所有
//        searchSourceBuilder.fetchSource(new String[]{"id","title"},null);
//        searchSourceBuilder.query(QueryBuilders.matchQuery("title","小米手机")); //分词查询
//        searchSourceBuilder.postFilter(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("brand","小米")).must(QueryBuilders.rangeQuery("price").gte(3000).lte(4000)));
//        searchSourceBuilder.from(0); //起始位置
//        searchSourceBuilder.size(2); //显示的条数

//        searchSourceBuilder.sort("price", SortOrder.DESC);

        searchSourceBuilder.query(QueryBuilders.termQuery("title","小米"));
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        highlightBuilder.field("title");
        searchSourceBuilder.highlighter(highlightBuilder);


        //把查询方式放入searchRequest
        searchRequest.source(searchSourceBuilder);
        //执行查询  返回searchResponse
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        System.out.println("查询总条数："+searchHits.getTotalHits());
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            Item item = gson.fromJson(sourceAsString, Item.class);

            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            Text[] fragments = highlightField.getFragments();
            if (fragments != null && fragments.length > 0) {
                String s = fragments[0].toString();
                item.setTitle(s);
            }


            System.out.println(item);
        }
    }

    @Test
    public void test() throws IOException {
        //构建大的查询对象  所有查询方式都能使用  可以指定索引库的名称
        SearchRequest searchRequest = new SearchRequest("leyou");
        //用来构建查询方式
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("brand_aggs").field("brand");
        searchSourceBuilder.aggregation(aggregationBuilder);

        //把查询方式放入searchRequest
        searchRequest.source(searchSourceBuilder);
        //执行查询  返回searchResponse
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        Aggregations aggregations = searchResponse.getAggregations();
        Terms terms = aggregations.get("brand_aggs");
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        for (Terms.Bucket bucket : buckets) {
            System.out.println( bucket.getKeyAsString()+":"+bucket.getDocCount());
        }
    }

    @After
    public void end() throws Exception {
        client.close();
    }
}
