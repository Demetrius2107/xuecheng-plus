package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.Exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDto;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {

    @Resource
    CourseBaseMapper courseBaseMapper;

    @Resource
    CourseMarketMapper courseMarketMapper;

    @Resource
    CourseCategoryMapper courseCategoryMapper;

    @Autowired
    CourseMarketServiceImpl courseMarketServiceImpl;

    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto queryCourseParamsDto) {
        //构建条件查询器
        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //构建查询条件:按照课程名称模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(queryCourseParamsDto.getCourseName()),CourseBase::getCompanyName,queryCourseParamsDto.getCourseName());
        //构建查询条件，按照课程审核状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,queryCourseParamsDto.getAuditStatus());
        //构建查询条件，按照课程发布状态查询
        queryWrapper.eq(StringUtils.isNotEmpty(queryCourseParamsDto.getPublishStatus()),CourseBase::getStatus,queryCourseParamsDto.getPublishStatus());
        //分页对象
        Page<CourseBase>  page = new Page<>(pageParams.getPageNo(),pageParams.getPageSize());
        //查询数据内容获取结果
        Page<CourseBase> pageInfo = courseBaseMapper.selectPage(page,queryWrapper);
        //获取数据列表
        List<CourseBase> items = pageInfo.getRecords();
        //获取数据总条数
        long counts = pageInfo.getTotal();
        //构建结果集
        return new PageResult<>(items,counts, pageParams.getPageNo(), pageParams.getPageSize());
    }

    @Override
    @Transactional
    public CourseBaseInfoDto createCourseBase(Long companyId, AddCourseDto addCourseDto) {
        // 1. 合法性校验
        if (StringUtils.isBlank(addCourseDto.getName())) {
            throw new RuntimeException("课程名称为空");
        }
        if (StringUtils.isBlank(addCourseDto.getMt())) {
            throw new RuntimeException("课程分类为空");
        }
        if (StringUtils.isBlank(addCourseDto.getSt())) {
            throw new RuntimeException("课程分类为空");
        }
        if (StringUtils.isBlank(addCourseDto.getGrade())) {
            throw new RuntimeException("课程等级为空");
        }
        if (StringUtils.isBlank(addCourseDto.getTeachmode())) {
            throw new RuntimeException("教育模式为空");
        }
        if (StringUtils.isBlank(addCourseDto.getUsers())) {
            throw new RuntimeException("适应人群为空");
        }
        if (StringUtils.isBlank(addCourseDto.getCharge())) {
            throw new RuntimeException("收费规则为空");
        }
        //2.封装课程基本信息
        //封装课程基本信息
        CourseBase courseBase = new CourseBase();
        BeanUtils.copyProperties(addCourseDto,courseBase);
        //2.1设置默认审核状态
        courseBase.setAuditStatus("202002");
        //2.2设置默认发布状态
        courseBase.setStatus("203001");
        //2.3设置机构id
        courseBase.setCompanyId(companyId);
        //2.4设置添加时间
        courseBase.setCreateDate(LocalDateTime.now());
        //2.5插入课程基本信息
        int baseInsert = courseBaseMapper.insert(courseBase);
        Long courseId = courseBase.getId();
        //封装课程营销信息
        CourseMarket courseMarket  = new CourseMarket();
        BeanUtils.copyProperties(addCourseDto,courseMarket);
        courseMarket.setId(courseId);
        //2.6判断收费规则，若课程收费，则价格必定大于0
        String charge = courseMarket.getCharge();
        if("201001".equals(charge)){
            Float price = addCourseDto.getPrice();
            if(price == null || price.floatValue() <= 0){
             //   throw new RuntimeException("课程设置了收费，价格不能为空，且必须大于0");
                XueChengPlusException.cast("课程设置了收费，价格不能为空，且必须要大于0");
            }
        }
        //2.7插入课程营销表
        int marketInsert = courseMarketMapper.insert(courseMarket);
        if(baseInsert <= 0 || marketInsert <= 0){
            throw new RuntimeException("新增课程基本信息失败");
        }
        //3.返回添加的课程信息
        return getCourseBaseInfo(courseId);
    }


    public CourseBaseInfoDto getCourseBaseInfo(Long courseId) {
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        //1.根据课程id查询课程基本信息
        CourseBase courseBase = new CourseBaseInfoDto();
        if (courseBase == null)
            return null;
        //1.1拷贝属性
        BeanUtils.copyProperties(courseBase, courseBaseInfoDto);
        //2根据课程id查询课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //2.1拷贝属性
        if (courseMarket != null)
            BeanUtils.copyProperties(courseMarket, courseBaseInfoDto);
            //3.查询课程分类名称、并设置属性
            //3.1根据小分类id查询课程分类对象
            CourseCategory courseCategoryBySy = courseCategoryMapper.selectById(courseBase.getSt());
            //3.2设置课程的小分类名称
            courseBaseInfoDto.setStName(courseCategoryBySy.getName());
            //3.3根据大分类id查询课程分类对象
            CourseCategory courseCategoryByMt = courseCategoryMapper.selectById(courseBase.getMt());
            //3.4设置课程大分类名称
            courseBaseInfoDto.setMtName(courseBaseInfoDto.getMtName());
            return courseBaseInfoDto;
    }

    @Override
    @Transactional
    public CourseBaseInfoDto updateCourseBase(Long companyId, EditCourseDto editCourseDto) {
        //判断当前修改课程是否属于当前机构
        Long courseId = editCourseDto.getId();
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if (!companyId.equals(courseBase.getCompanyId())) {
            XueChengPlusException.cast("只允许修改本机构的课程");
        }
        //拷贝对象
        BeanUtils.copyProperties(editCourseDto, courseBase);
        //更新，设置更新时间
        courseBase.setChangeDate(LocalDateTime.now());
        courseBaseMapper.updateById(courseBase);
        //查询课程营销信息
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        //由于课程营销信息不是必填项，所以先进行判断
        if (courseMarket == null) {
            courseMarket = new CourseMarket();
        }
        courseMarket.setId(courseId);
        //获取课程收费状态并设置
        String charge = editCourseDto.getCharge();
        courseMarket.setCharge(charge);
        //如果课程收费，则判断价格是否正常
        if (charge.equals("201001")) {
            Float price = editCourseDto.getPrice();
            if (price <= 0 || price == null) {
                XueChengPlusException.cast("课程设置了收费，价格不能为空，且必须把大于0");
            }
        }
        //对象拷贝
        BeanUtils.copyProperties(editCourseDto, courseMarket);
        //有则更新，无则插入

        //courseMarket.setId(courseId);
        //获取课程收费状态并设置
        this.saveCourseMarket(courseMarket);
        return getCourseBaseInfo(courseId);
    }

    private int saveCourseMarket(CourseMarket courseMarket) {
        String charge = courseMarket.getCharge();
        if (StringUtils.isBlank(charge))
            XueChengPlusException.cast("请设置收费规则");
        if (charge.equals("201001")) {
            Float price = courseMarket.getPrice();
            if (price == null || price <= 0) {
                XueChengPlusException.cast("课程设置了收费，价格不能为空，且必须大于0");
            }
        }
        // 2.7 插入课程营销信息表
        boolean flag = courseMarketServiceImpl.saveOrUpdate(courseMarket);
        return flag ? 1 : -1;
    }



}
