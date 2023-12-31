package com.practice.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.practice.reggie.mapper.addressBookMapper;
import com.practice.reggie.model.AddressBook;
import com.practice.reggie.model.User;
import com.practice.reggie.service.addressBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.LongAccumulator;

@Service
public class addressBookServiceImpl extends ServiceImpl<addressBookMapper, AddressBook> implements addressBookService{
    @Autowired
    private userServiceImpl userService;

    /**
     * 根据user，获取到它所有的地址
     * @param user
     * @return
     */
    @Override
    public List<AddressBook> selectAllAddressByUser(User user) {
        //1.拿到电话号码
        String phone = user.getPhone();
        //2.根据电话号码拿到userId，确定唯一身份 //使用LocalThread更好
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        //用户处于可用状态
        queryWrapper.eq(User::getStatus, 1);
        User one = userService.getOne(queryWrapper,false);
        //3.根据userId查出属于该用户的所有地址
        LambdaQueryWrapper<AddressBook> addressBookLambdaQueryWrapper = new LambdaQueryWrapper<>();
        addressBookLambdaQueryWrapper.eq(AddressBook::getUserId,one.getId());
        //没有被删除
        addressBookLambdaQueryWrapper.eq(AddressBook::getIsDeleted, 0);
        List<AddressBook> list = this.list(addressBookLambdaQueryWrapper);
        //4.返回地址
        return list;
    }

    /**
     * 将当前地址对象设置为默认地址
     * @param addressBook
     * @return
     */
    @Override
    public boolean updateDefaultWithOtherChange(AddressBook addressBook) {
        //1.拿到地址对象的id,再拿到用户id
        Long id = addressBook.getId();
        AddressBook byId = super.getById(id);
        //2.将该地址对应的用户其他的地址改为非默认
        LambdaQueryWrapper<AddressBook> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(AddressBook::getUserId,byId.getUserId());
        queryWrapper.eq(AddressBook::getIsDeleted,0);
        queryWrapper.eq(AddressBook::getIsDefault,1);

        AddressBook one = super.getOne(queryWrapper);
        if (one != null) {
            one.setIsDefault(0);
            boolean updateCurrentDefault = super.updateById(one);

            addressBook.setIsDefault(1);
            boolean updateNewDefault = super.updateById(addressBook);

            return updateCurrentDefault && updateNewDefault;
        } else {
            // 处理查询没有结果的情况，可能是因为找不到符合条件的默认地址
            return false;
        }
    }
}
