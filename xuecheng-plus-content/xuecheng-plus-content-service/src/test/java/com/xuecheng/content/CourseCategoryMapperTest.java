package com.xuecheng.content;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;


@SpringBootTest
public class CourseCategoryMapperTest {

   @Autowired
   CourseCategoryMapper courseCategoryMapper;
    @Test
   public void contextCourseCategoryTest(){
        List<CourseCategoryTreeDto> courseCategoryTreeDtos =courseCategoryMapper.selectTreeNodes("1");
        System.out.println(courseCategoryTreeDtos);
    }
}
