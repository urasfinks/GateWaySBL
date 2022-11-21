package ru.jamsys.sbl.jpa.service;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Service
public class NoCache {
    @PersistenceContext
    private EntityManager em;

    @Transactional
    protected <T> T saveWithoutCache(CrudRepository<T, Long> crudRepository, T entity) {
        //Это самое больше зло, с чем я встречался
        T ret = crudRepository.save(entity);
        try {
            em.flush();
        } catch (Exception e) {
        }
        return ret;
    }
}
