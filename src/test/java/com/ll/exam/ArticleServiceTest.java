package com.ll.exam;

import com.ll.exam.article.dto.ArticleDto;
import com.ll.exam.article.service.ArticleService;
import com.ll.exam.mymap.MyMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArticleServiceTest {
    private MyMap myMap;
    private ArticleService articleService;
    private static final int TEST_DATA_SIZE = 30;

    public ArticleServiceTest() {
        myMap = Container.getObj(MyMap.class);
        articleService = Container.getObj(ArticleService.class);
    }
    @BeforeAll
    public void beforeAll() {
        myMap.setDevMode(true);
    }

    @BeforeEach
    public void beforeEach() {
        truncateArticleTable();
        makeArticleTestData();
    }

    private void makeArticleTestData() {
        IntStream.rangeClosed(1, TEST_DATA_SIZE).forEach(no -> {
            boolean isBlind = no >=11 && no <=20;
            String title = "제목%d".formatted(no);
            String body = "내용%d".formatted(no);

            myMap.run("""
                    INSERT INTO article
                    SET createdDate = NOW(),
                    modifiedDate = NOW(),
                    title = ?,
                    `body` = ?,
                    isBlind = ?
                    """, title, body, isBlind);
        });
    }

    private void truncateArticleTable() {
        myMap.run("TRUNCATE article");
    }

    @Test
    public void articleService가_존재한다() {

        assertThat(articleService).isNotNull();
    }

    @Test
    public void getArticles() {

        List<ArticleDto> articleDtoList=articleService.getArticles();
        assertThat(articleDtoList.size()).isEqualTo(TEST_DATA_SIZE);
    }

    @Test
    public void getArticlesById() {
        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1L);
        assertThat(articleDto.getTitle()).isEqualTo("제목1");
        assertThat(articleDto.getBody()).isEqualTo("내용1");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isFalse();
    }

    @Test
    public void getArticlesCount() {
        long articlesCount = articleService.getArticlesCount();

        assertThat(articlesCount).isEqualTo(TEST_DATA_SIZE);
    }

    @Test
    public void write() {
        long newArticleId = articleService.write("제목 new", "내용 new", false);
        
        ArticleDto articleDto = articleService.getArticleById(newArticleId);

        assertThat(articleDto.getId()).isEqualTo(newArticleId);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.getCreatedDate()).isNotNull();
        assertThat(articleDto.getModifiedDate()).isNotNull();
        assertThat(articleDto.isBlind()).isEqualTo(false);
    }

    @Test
    public void modify() {
        articleService.modify(1, "제목 new", "내용 new", true);

        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto.getId()).isEqualTo(1);
        assertThat(articleDto.getTitle()).isEqualTo("제목 new");
        assertThat(articleDto.getBody()).isEqualTo("내용 new");
        assertThat(articleDto.isBlind()).isEqualTo(true);

        long diffSeconds = ChronoUnit.SECONDS.between(articleDto.getModifiedDate(), LocalDateTime.now());
        assertThat(diffSeconds).isLessThanOrEqualTo(1L);
    }

    @Test
    public void delete() {
        articleService.delete(1);

        ArticleDto articleDto = articleService.getArticleById(1);

        assertThat(articleDto).isNull();

    }

    @Test
    public void 다음글_가져오기() {


        ArticleDto articleDto3 = articleService.getArticleById(2);
        ArticleDto articleDto2 = articleService.selectnextArticle(3);

        assertThat(articleDto2.getId()).isEqualTo(3);

    }

    @Test
    public void 이전글_가져오기() {
        ArticleDto articleDto2 = articleService.getArticleById(3);
        ArticleDto articleDto1 = articleService.selectpreviousArticle(2);


        assertThat(articleDto1.getId()).isEqualTo(2);

    }

    @Test
    public void 마지막글의_다음글은_없다() {
        long lastArticleDto = TEST_DATA_SIZE;
        ArticleDto nullArticleDto = articleService.getArticleById(lastArticleDto);

        assertThat(nullArticleDto).isNull();
    }

    @Test
    public void _10번글의_다음글은_21번글_11번부터_20글까지는_블라인드() {
        ArticleDto articleDto1 = articleService.selectnextArticle(10);


        assertThat(articleDto1.getId()).isEqualTo(21);
    }
}
