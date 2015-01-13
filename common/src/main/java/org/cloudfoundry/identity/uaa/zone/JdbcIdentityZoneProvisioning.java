/*******************************************************************************
 *     Cloud Foundry
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.zone;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

public class JdbcIdentityZoneProvisioning implements IdentityZoneProvisioning {

    public static final String ID_ZONE_FIELDS = "id,version,created,lastModified,name,subdomain,description";

    public static final String ID_ZONE_UPDATE_FIELDS = ID_ZONE_FIELDS.substring(3).replace(",","=?,")+"=?";

    public static final String CREATE_IDENTITY_ZONE_SQL = "insert into identity_zone(" + ID_ZONE_FIELDS + ") values (?,?,?,?,?,?,?)";

    public static final String UPDATE_IDENTITY_ZONE_SQL = "update identity_zone set " + ID_ZONE_UPDATE_FIELDS + " where id=?";
    
    public static final String IDENTITY_ZONES_QUERY = "select " + ID_ZONE_FIELDS + " from identity_zone ";

    public static final String IDENTITY_ZONE_BY_ID_QUERY = IDENTITY_ZONES_QUERY + "where id=?";
    
    public static final String IDENTITY_ZONE_BY_SUBDOMAIN_QUERY = "select " + ID_ZONE_FIELDS + " from identity_zone " + "where subdomain=?";

    protected final JdbcTemplate jdbcTemplate;

    private final RowMapper<IdentityZone> mapper = new IdentityZoneRowMapper();

    public JdbcIdentityZoneProvisioning(JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public IdentityZone retrieve(String id) {
        try {
            IdentityZone identityZone = jdbcTemplate.queryForObject(IDENTITY_ZONE_BY_ID_QUERY, mapper, id);
            return identityZone;
        } catch (EmptyResultDataAccessException x) {
            throw new ZoneDoesNotExistsException("Zone["+id+"] not found.", x);
        }
    }
    
    @Override
    public List<IdentityZone> retrieveAll() {
        return jdbcTemplate.query(IDENTITY_ZONES_QUERY, mapper);
    }

    @Override
    public IdentityZone retrieveBySubdomain(String subdomain) {
        IdentityZone identityZone = jdbcTemplate.queryForObject(IDENTITY_ZONE_BY_SUBDOMAIN_QUERY, mapper, subdomain);
        return identityZone;
    }

    @Override
    public IdentityZone create(final IdentityZone identityZone) {

        try {
            jdbcTemplate.update(CREATE_IDENTITY_ZONE_SQL, new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    ps.setString(1, identityZone.getId().trim());
                    ps.setInt(2, identityZone.getVersion());
                    ps.setTimestamp(3, new Timestamp(new Date().getTime()));
                    ps.setTimestamp(4, new Timestamp(new Date().getTime()));
                    ps.setString(5, identityZone.getName());
                    ps.setString(6, identityZone.getSubdomain());
                    ps.setString(7, identityZone.getDescription());
                }
            });
        } catch (DuplicateKeyException e) {
            throw new ZoneAlreadyExistsException(e.getMostSpecificCause().getMessage(), e);
        }

        return retrieve(identityZone.getId());
    }

    @Override
    public IdentityZone update(final IdentityZone identityZone) {

        try {
            jdbcTemplate.update(UPDATE_IDENTITY_ZONE_SQL, new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps) throws SQLException {
                    ps.setInt(1, identityZone.getVersion());
                    ps.setTimestamp(2, new Timestamp(new Date().getTime()));
                    ps.setTimestamp(3, new Timestamp(new Date().getTime()));
                    ps.setString(4, identityZone.getName());
                    ps.setString(5, identityZone.getSubdomain());
                    ps.setString(6, identityZone.getDescription());
                    ps.setString(7, identityZone.getId().trim());
                }
            });
        } catch (DuplicateKeyException e) {
            //duplicate subdomain
            throw new ZoneAlreadyExistsException(e.getMostSpecificCause().getMessage(), e);
        }
        return retrieve(identityZone.getId());
    }

    private static final class IdentityZoneRowMapper implements RowMapper<IdentityZone> {
        @Override
        public IdentityZone mapRow(ResultSet rs, int rowNum) throws SQLException {

            IdentityZone identityZone = new IdentityZone();

            identityZone.setId(rs.getString(1).trim());
            identityZone.setVersion(rs.getInt(2));
            identityZone.setCreated(rs.getTimestamp(3));
            identityZone.setLastModified(rs.getTimestamp(4));
            identityZone.setName(rs.getString(5));
            identityZone.setSubdomain(rs.getString(6));
            identityZone.setDescription(rs.getString(7));

            return identityZone;
        }
    }

}
