/*
 * Copyright [2022] [https://www.xiaonuo.vip]
 *
 * Snowy采用APACHE LICENSE 2.0开源协议，您在使用过程中，需要注意以下几点：
 *
 * 1.请不要删除和修改根目录下的LICENSE文件。
 * 2.请不要删除和修改Snowy源码头部的版权声明。
 * 3.本项目代码可免费商业使用，商业使用请保留源码和相关描述文件的项目出处，作者声明等。
 * 4.分发源码时候，请注明软件出处 https://www.xiaonuo.vip
 * 5.不可二次分发开源参与同类竞品，如有想法可联系团队xiaonuobase@qq.com商议合作。
 * 6.若您的项目无法满足以上几点，需要更多功能代码，获取Snowy商业授权许可，请在官网购买授权，地址为 https://www.xiaonuo.vip
 */
package vip.xiaonuo.mobile.modular.resource.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import vip.xiaonuo.common.enums.CommonSortOrderEnum;
import vip.xiaonuo.common.exception.CommonException;
import vip.xiaonuo.common.page.CommonPageRequest;
import vip.xiaonuo.mobile.modular.resource.entity.MobileModule;
import vip.xiaonuo.mobile.modular.resource.enums.MobileResourceCategoryEnum;
import vip.xiaonuo.mobile.modular.resource.mapper.MobileModuleMapper;
import vip.xiaonuo.mobile.modular.resource.param.module.MobileModuleAddParam;
import vip.xiaonuo.mobile.modular.resource.param.module.MobileModuleEditParam;
import vip.xiaonuo.mobile.modular.resource.param.module.MobileModuleIdParam;
import vip.xiaonuo.mobile.modular.resource.param.module.MobileModulePageParam;
import vip.xiaonuo.mobile.modular.resource.service.MobileMenuService;
import vip.xiaonuo.mobile.modular.resource.service.MobileModuleService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 模块Service接口实现类
 *
 * @author xuyuxiang
 * @date 2022/6/27 14:25
 **/
@Service
public class MobileModuleServiceImpl extends ServiceImpl<MobileModuleMapper, MobileModule> implements MobileModuleService {

    @Resource
    private MobileMenuService mobileMenuService;

    /*
    @Resource
    private MobileRelationService mobileRelationService;
    */

    @Override
    public Page<MobileModule> page(MobileModulePageParam mobileModulePageParam) {
        QueryWrapper<MobileModule> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(MobileModule::getCategory, MobileResourceCategoryEnum.MODULE.getValue());
        if(ObjectUtil.isNotEmpty(mobileModulePageParam.getSearchKey())) {
            queryWrapper.lambda().like(MobileModule::getTitle, mobileModulePageParam.getSearchKey());
        }
        if(ObjectUtil.isAllNotEmpty(mobileModulePageParam.getSortField(), mobileModulePageParam.getSortOrder())) {
            CommonSortOrderEnum.validate(mobileModulePageParam.getSortOrder());
            queryWrapper.orderBy(true, mobileModulePageParam.getSortOrder().equals(CommonSortOrderEnum.ASC.getValue()),
                    StrUtil.toUnderlineCase(mobileModulePageParam.getSortField()));
        } else {
            queryWrapper.lambda().orderByAsc(MobileModule::getSortCode);
        }
        return this.page(CommonPageRequest.defaultPage(), queryWrapper);
    }

    @Override
    public void add(MobileModuleAddParam mobileModuleAddParam) {
        MobileModule mobileModule = BeanUtil.toBean(mobileModuleAddParam, MobileModule.class);
        boolean repeatTitle = this.count(new LambdaQueryWrapper<MobileModule>().eq(MobileModule::getCategory,
                MobileResourceCategoryEnum.MODULE.getValue()).eq(MobileModule::getTitle, mobileModule.getTitle())) > 0;
        if(repeatTitle) {
            throw new CommonException("存在重复的模块，名称为：{}", mobileModule.getTitle());
        }
        mobileModule.setCode(RandomUtil.randomString(10));
        mobileModule.setCategory(MobileResourceCategoryEnum.MODULE.getValue());
        this.save(mobileModule);
    }

    @Override
    public void edit(MobileModuleEditParam mobileModuleEditParam) {
        MobileModule mobileModule = this.queryEntity(mobileModuleEditParam.getId());
        BeanUtil.copyProperties(mobileModuleEditParam, mobileModule);
        boolean repeatTitle = this.count(new LambdaQueryWrapper<MobileModule>().eq(MobileModule::getCategory,
                MobileResourceCategoryEnum.MODULE.getValue()).eq(MobileModule::getTitle, mobileModule.getTitle())
                .ne(MobileModule::getId, mobileModule.getId())) > 0;
        if(repeatTitle) {
            throw new CommonException("存在重复的模块，名称为：{}", mobileModule.getTitle());
        }
        this.updateById(mobileModule);
    }

    @Override
    public void delete(List<MobileModuleIdParam> mobileModuleIdParamList) {
        /*List<String> mobileModuleIdList = CollStreamUtil.toList(mobileModuleIdParamList, MobileModuleIdParam::getId);
        if(ObjectUtil.isNotEmpty(mobileModuleIdList)) {
            boolean containsMobiletemModule = this.listByIds(mobileModuleIdList).stream().map(MobileModule::getCode)
                    .collect(Collectors.toSet()).contains(MobileBuildInEnum.BUILD_IN_MODULE_CODE.getValue());
            if(containsMobiletemModule) {
                throw new CommonException("不可删除系统内置模块");
            }

            // 获取模块下的菜单
            List<MobileMenu> allMenuList = mobileMenuService.list(new LambdaUpdateWrapper<MobileMenu>()
                    .in(MobileMenu::getCategory, CollectionUtil.newArrayList(MobileResourceCategoryEnum.MENU.getValue())));
            if(ObjectUtil.isNotEmpty(allMenuList)) {
                List<String> toDeleteMenuIdList = CollectionUtil.newArrayList(mobileModuleIdList);
                allMenuList.stream().filter(mobileMenu -> mobileModuleIdList.contains(mobileMenu.getModule()))
                        .collect(Collectors.toList()).forEach(mobileMenu -> toDeleteMenuIdList
                        .addAll(mobileMenuService.getChildListById(allMenuList, mobileMenu.getId(), true).stream()
                                .map(MobileMenu::getId).collect(Collectors.toList())));
                if(ObjectUtil.isNotEmpty(toDeleteMenuIdList)) {
                    // 清除对应的角色与移动端资源信息
                    mobileRelationService.remove(new LambdaUpdateWrapper<MobileRelation>().in(MobileRelation::getTargetId, toDeleteMenuIdList)
                            .eq(MobileRelation::getCategory, MobileRelationCategoryEnum.MOBILE_ROLE_HAS_RESOURCE.getValue()));
                    // 执行删除
                    this.removeBatchByIds(toDeleteMenuIdList);
                }
            }
        }*/
        // TODO
    }

    @Override
    public MobileModule detail(MobileModuleIdParam mobileModuleIdParam) {
        return this.queryEntity(mobileModuleIdParam.getId());
    }

    @Override
    public MobileModule queryEntity(String id) {
        MobileModule mobileModule = this.getById(id);
        if(ObjectUtil.isEmpty(mobileModule)) {
            throw new CommonException("模块不存在，id值为：{}", id);
        }
        return mobileModule;
    }
}
