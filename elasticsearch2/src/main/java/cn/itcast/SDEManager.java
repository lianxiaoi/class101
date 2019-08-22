package cn.itcast;

import cn.itcast.pojo.Goods;
import com.google.gson.Gson;
import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SDEManager {

    @Autowired
    private ElasticsearchTemplate esTemplate;

    @Autowired
    private GoodsRepository goodsRepository;


    @Test
    public void testDoc() {
//        Goods goods = new Goods(1L, "小米6X 手机", "手机", "小米", 1199.0, "weehtgc");
//        goodsRepository.save(goods);

        // 删除
//        goodsRepository.deleteById(1L);

        // 准备文档数据：
        List<Goods> list = new ArrayList<>();
        list.add(new Goods(1L, "小米手机7", "手机", "小米", 3299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Goods(2L, "坚果手机R1", "手机", "锤子", 3699.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Goods(3L, "华为META10", "手机", "华为", 4499.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Goods(4L, "小米Mix2S", "手机", "小米", 4299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Goods(5L, "荣耀V10", "手机", "华为", 2799.00, "http://image.leyou.com/13123.jpg"));
        goodsRepository.saveAll(list); //批量新增

    }

    @Test
    public void testQuery() {
//        Iterable<Goods> all = goodsRepository.findAll();
//        for (Goods goods : all) {
//            System.out.println(goods);
//        }

//        Optional<Goods> optional = goodsRepository.findById(1L);
//        Goods goods = optional.get();
//        System.out.println(goods);

        //分页查询
        /*Page<Goods> page = goodsRepository.findAll(PageRequest.of(1, 4));
        List<Goods> goodsList = page.getContent();
        for (Goods goods : goodsList) {
            System.out.println(goods);
        }*/

        List<Goods> goodsList = goodsRepository.findByTitle("小米");
        for (Goods goods : goodsList) {
            System.out.println(goods);
        }
    }

    @Test
    public void nativeQuery() {

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withQuery(QueryBuilders.termQuery("title", "坚果"));

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        highlightBuilder.field("title");

        nativeSearchQueryBuilder.withHighlightBuilder(highlightBuilder);
        nativeSearchQueryBuilder.withHighlightFields(new HighlightBuilder.Field("title"));

        AggregatedPage<Goods> page = esTemplate.queryForPage(nativeSearchQueryBuilder.build(), Goods.class, new SearchResultMapperImpl<Goods>());
        List<Goods> goodsList = page.getContent();
        for (Goods goods : goodsList) {
            System.out.println(goods);
        }

    }

    @Test
    public void createIndex() {
        //创建索引库
        esTemplate.createIndex(Goods.class);
        //创建映射
        esTemplate.putMapping(Goods.class);
    }

    Gson gson = new Gson();

    class SearchResultMapperImpl<T> implements SearchResultMapper {

        @Override
        public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
            long totalHits = searchResponse.getHits().getTotalHits();
            Aggregations aggregations = searchResponse.getAggregations();
            String scrollId = searchResponse.getScrollId();
            float maxScore = searchResponse.getHits().getMaxScore();

            List<T> content = new ArrayList<T>();
            SearchHit[] hits = searchResponse.getHits().getHits();
            for (SearchHit hit : hits) {
                String sourceAsString = hit.getSourceAsString();
                T t = gson.fromJson(sourceAsString, aClass);

                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                Text[] fragments = highlightField.getFragments();
                if (fragments != null && fragments.length > 0) {
                    String title_highLight = fragments[0].toString();
                    // t.setTitle(title_highLight);
                    try {
                        BeanUtils.setProperty(t, "title", title_highLight); //BeanUtils来自commons-beanutils的包
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                content.add(t);
            }


            return new AggregatedPageImpl<T>(content, pageable, totalHits, aggregations, scrollId, maxScore);
        }
    }

}
