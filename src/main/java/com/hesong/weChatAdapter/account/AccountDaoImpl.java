package com.hesong.weChatAdapter.account;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;


public class AccountDaoImpl extends HibernateDaoSupport implements AccountDao {
    private static Logger log = Logger.getLogger(AccountDaoImpl.class);
    @Override
    public void save(Account a) {
        try {
            getHibernateTemplate().save(a);

        } catch (Exception e) {
            log.error("Svae account error, cuased by: "
                    + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void saveAll(List<Account> list) {
        try {
            getHibernateTemplate().saveOrUpdateAll(list);

        } catch (Exception e) {
            log.error("Svae account list error, cuased by: "
                    + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void update(Account a) {
        getHibernateTemplate().update(a);
    }

    @Override
    public void delete(Account a) {
        getHibernateTemplate().delete(a);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List getAllAccount() {
        try {
            List list = getHibernateTemplate().find("from Account");
            log.info("Account size: "+list.size());
            return list;
        } catch (Exception e) {
            log.error("Get account list failed, caused by: "+e.toString());
            e.printStackTrace();
        }
        return new ArrayList();

    }

    @SuppressWarnings("rawtypes")
    @Override
    public List findByAcctype(String acctype) {
        List list = getHibernateTemplate().find(
                "from Account a where a.acctype=?", acctype);
        return list.size() > 0 ? list : null;
    }

}
