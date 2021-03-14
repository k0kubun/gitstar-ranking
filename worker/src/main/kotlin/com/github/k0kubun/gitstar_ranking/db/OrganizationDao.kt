package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.core.Organization
import java.sql.ResultSet
import org.skife.jdbi.v2.StatementContext
import org.skife.jdbi.v2.sqlobject.Bind
import org.skife.jdbi.v2.sqlobject.SqlQuery
import org.skife.jdbi.v2.sqlobject.customizers.Mapper
import org.skife.jdbi.v2.tweak.ResultSetMapper

interface OrganizationDao {
    @SqlQuery("select id, login, type, stargazers_count, updated_at from users where type = 'Organization' order by stargazers_count desc, id desc limit :limit")
    @Mapper(OrganizationMapper::class)
    fun starsDescFirstOrgs(@Bind("limit") limit: Int?): List<Organization>

    @SqlQuery("select id, login, type, stargazers_count, updated_at from users where type = 'Organization' and " +
        "(stargazers_count, id) < (:stargazersCount, :id) order by stargazers_count desc, id desc limit :limit")
    @Mapper(OrganizationMapper::class)
    fun starsDescOrgsAfter(@Bind("stargazersCount") stargazersCount: Int?, @Bind("id") id: Int?, @Bind("limit") limit: Int?): List<Organization>

    @SqlQuery("select count(1) from users where type = 'Organization'")
    fun countOrganizations(): Int

    @SqlQuery("select count(1) from users where type = 'Organization' and stargazers_count = :stargazersCount")
    fun countOrganizationsHavingStars(@Bind("stargazersCount") stargazersCount: Int): Int
    class OrganizationMapper : ResultSetMapper<Organization> {
        override fun map(index: Int, r: ResultSet, ctx: StatementContext): Organization {
            return Organization(
                id = r.getInt("id"),
                login = r.getString("login"),
                stargazersCount = r.getInt("stargazers_count"),
                updatedAt = r.getTimestamp("updated_at"),
            )
        }
    }
}
