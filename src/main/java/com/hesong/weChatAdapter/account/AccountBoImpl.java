package com.hesong.weChatAdapter.account;

import java.util.List;

public class AccountBoImpl implements AccountBo{

    AccountDao accountDao; 
    
    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public void save(Account a) {
        this.accountDao.save(a);
    }

    @Override
    public void update(Account a) {
        this.accountDao.update(a);
    }

    @Override
    public void delete(Account a) {
        this.accountDao.delete(a);
    }


    @SuppressWarnings("rawtypes")
    @Override
    public List getAllAccount() {
        return this.accountDao.getAllAccount();
    }

    @Override
    public void saveAll(List<Account> list) {
        this.accountDao.saveAll(list);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List findByAcctype(String acctype) {
        return this.accountDao.findByAcctype(acctype);
    }
    
}
