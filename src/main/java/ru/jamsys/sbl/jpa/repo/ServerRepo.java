package ru.jamsys.sbl.jpa.repo;

import org.hibernate.annotations.Immutable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.jamsys.sbl.jpa.dto.ServerDTO;
import ru.jamsys.sbl.jpa.dto.custom.ServerStatistic;

import javax.persistence.LockModeType;
import java.util.List;

@Repository
public interface ServerRepo extends CrudRepository<ServerDTO, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("select t from ServerDTO t where t.status = 0 and t.pingStatus = 1 order by t.id asc")
    List<ServerDTO> getAlready();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ServerDTO t where t.id = :id_server")
    ServerDTO findOneForUpdate(@Param("id_server") Long idServer);

    /*
SELECT s2.* FROM srv s2
INNER JOIN (
	SELECT
	 s1.id_srv,
	 s1.max_count_v_srv,
	 sq1.count_v_srv,
	 (s1.max_count_v_srv - CASE WHEN sq1.count_v_srv IS NOT NULL THEN sq1.count_v_srv ELSE 0 END) AS diff
	FROM srv s1
	LEFT JOIN (
		SELECT vs1.id_srv, count(vs1.*) AS count_v_srv FROM v_srv vs1
		WHERE vs1.status_v_srv >= 0
		GROUP BY vs1.id_srv
	) AS sq1 ON sq1.id_srv = s1.id_srv
) AS sq2 ON sq2.id_srv = s2.id_srv AND sq2.diff > 0 AND s2.ping_status_srv = 1
    * */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "SELECT s2.* FROM srv s2\n" +
            "INNER JOIN (\n" +
            "\tSELECT \n" +
            "\t s1.id_srv,\n" +
            "\t s1.max_count_v_srv,\n" +
            "\t sq1.count_v_srv,\n" +
            "\t (s1.max_count_v_srv - CASE WHEN sq1.count_v_srv IS NOT NULL THEN sq1.count_v_srv ELSE 0 END) AS diff\n" +
            "\tFROM srv s1\n" +
            "\tLEFT JOIN (\n" +
            "\t\tSELECT vs1.id_srv, count(vs1.*) AS count_v_srv FROM v_srv vs1\n" +
            "\t\tWHERE vs1.status_v_srv >= 0\n" +
            "\t\tGROUP BY vs1.id_srv\n" +
            "\t) AS sq1 ON sq1.id_srv = s1.id_srv\n" +
            ") AS sq2 ON sq2.id_srv = s2.id_srv AND sq2.diff > 0 AND s2.ping_status_srv = 1", nativeQuery = true)
    List<ServerDTO> getAvailable();

    /*
SELECT 0 AS ID_SRV,
	'' AS NAME_SRV,
	MAX(S1.DATE_ADD_SRV) AS DATE_ADD_SRV,
	MAX(S1.IP_SRV) AS IP_SRV,
	MAX(S1.STATUS_SRV) AS STATUS_SRV,
	MAX(S1.PING_DATE_SRV) AS PING_DATE_SRV,
	MAX(S1.ID_ROUTER) AS ID_ROUTER,
	MAX(S1.PING_STATUS_SRV) AS PING_STATUS_SRV,
	MAX(S1.ID_TASK) AS ID_TASK,
	MAX(S1.LOCK_DATE_SRV) AS LOCK_DATE_SRV,
	MAX(S1.PORT_SRV) AS PORT_SRV,
	SUM(S1.MAX_COUNT_V_SRV) AS MAX_COUNT_V_SRV,
	MAX(S1.TRY_PING_DATE_SRV) AS TRY_PING_DATE_SRV,
	SUM(CASE WHEN SQ1.AV IS NULL THEN 0 ELSE SQ1.AV END) || '' AS TMP
FROM SRV S1
LEFT JOIN
	(SELECT VS1.ID_SRV,
			COUNT(*) AS AV
		FROM V_SRV VS1
		WHERE VS1.STATUS_V_SRV >= 0
		GROUP BY VS1.ID_SRV) AS SQ1 ON SQ1.ID_SRV = S1.ID_SRV
WHERE 1 = 1
	AND S1.PING_STATUS_SRV = 1
*/
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "SELECT 0 AS ID_SRV,\n" +
            "\t'' AS NAME_SRV,\n" +
            "\tMAX(S1.DATE_ADD_SRV) AS DATE_ADD_SRV,\n" +
            "\tMAX(S1.IP_SRV) AS IP_SRV,\n" +
            "\tMAX(S1.STATUS_SRV) AS STATUS_SRV,\n" +
            "\tMAX(S1.PING_DATE_SRV) AS PING_DATE_SRV,\n" +
            "\tMAX(S1.ID_ROUTER) AS ID_ROUTER,\n" +
            "\tMAX(S1.PING_STATUS_SRV) AS PING_STATUS_SRV,\n" +
            "\tMAX(S1.ID_TASK) AS ID_TASK,\n" +
            "\tMAX(S1.LOCK_DATE_SRV) AS LOCK_DATE_SRV,\n" +
            "\tMAX(S1.PORT_SRV) AS PORT_SRV,\n" +
            "\tSUM(S1.MAX_COUNT_V_SRV) AS MAX_COUNT_V_SRV,\n" +
            "\tMAX(S1.TRY_PING_DATE_SRV) AS TRY_PING_DATE_SRV,\n" +
            "\tSUM(CASE WHEN SQ1.AV IS NULL THEN 0 ELSE SQ1.AV END) || '' AS TMP\n" +
            "FROM SRV S1\n" +
            "LEFT JOIN\n" +
            "\t(SELECT VS1.ID_SRV,\n" +
            "\t\t\tCOUNT(*) AS AV\n" +
            "\t\tFROM V_SRV VS1\n" +
            "\t\tWHERE VS1.STATUS_V_SRV >= 0\n" +
            "\t\tGROUP BY VS1.ID_SRV) AS SQ1 ON SQ1.ID_SRV = S1.ID_SRV\n" +
            "WHERE 1 = 1\n" +
            "\tAND S1.PING_STATUS_SRV = 1", nativeQuery = true)
    List<ServerDTO> getAvgAvailable();

}
