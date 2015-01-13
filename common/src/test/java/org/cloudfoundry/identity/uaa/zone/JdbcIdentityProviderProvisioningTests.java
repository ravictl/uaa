package org.cloudfoundry.identity.uaa.zone;

import org.apache.commons.lang.RandomStringUtils;
import org.cloudfoundry.identity.uaa.test.JdbcTestBase;
import org.cloudfoundry.identity.uaa.test.NullSafeSystemProfileValueSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class JdbcIdentityProviderProvisioningTests extends JdbcTestBase {

    private JdbcIdentityProviderProvisioning db;

    @Before
    public void createDatasource() throws Exception {
        IdentityZoneHolder.clear();
        db = new JdbcIdentityProviderProvisioning(jdbcTemplate);
    }
    
    @After
    public void cleanUp() {
        IdentityZoneHolder.clear();
    }

    @Test
    public void testCreateIdentityProviderInDefaultZone() throws Exception {
        String originKey = RandomStringUtils.randomAlphabetic(6);
        IdentityProvider idp = MultitenancyFixture.identityProvider(originKey);

        IdentityProvider createdIdp = db.create(idp);
        Map<String, Object> rawCreatedIdp = jdbcTemplate.queryForMap("select * from identity_provider where id = ?",createdIdp.getId());
        
        assertEquals(idp.getName(), createdIdp.getName());
        assertEquals(idp.getOriginKey(), createdIdp.getOriginKey());
        assertEquals(idp.getType(), createdIdp.getType());
        assertEquals(idp.getConfig(), createdIdp.getConfig());
        
        assertEquals(idp.getName(), rawCreatedIdp.get("name"));
        assertEquals(idp.getOriginKey(), rawCreatedIdp.get("origin_key"));
        assertEquals(idp.getType(), rawCreatedIdp.get("type"));
        assertEquals(idp.getConfig(), rawCreatedIdp.get("config"));
        assertEquals(IdentityZoneHolder.get().getId(), rawCreatedIdp.get("identity_zone_id").toString().trim());
    }
    
    @Test
    public void testCreateIdentityProviderInOtherZone() throws Exception {
        IdentityZoneHolder.set(MultitenancyFixture.identityZone(UUID.randomUUID().toString(),"myzone"));
        String originKey = RandomStringUtils.randomAlphabetic(6);
        IdentityProvider idp = MultitenancyFixture.identityProvider(originKey);

        IdentityProvider createdIdp = db.create(idp);
        Map<String, Object> rawCreatedIdp = jdbcTemplate.queryForMap("select * from identity_provider where id = ?",createdIdp.getId());
        
        assertEquals(idp.getName(), createdIdp.getName());
        assertEquals(idp.getOriginKey(), createdIdp.getOriginKey());
        assertEquals(idp.getType(), createdIdp.getType());
        assertEquals(idp.getConfig(), createdIdp.getConfig());
        
        assertEquals(idp.getName(), rawCreatedIdp.get("name"));
        assertEquals(idp.getOriginKey(), rawCreatedIdp.get("origin_key"));
        assertEquals(idp.getType(), rawCreatedIdp.get("type"));
        assertEquals(idp.getConfig(), rawCreatedIdp.get("config"));
        assertEquals(IdentityZoneHolder.get().getId(), rawCreatedIdp.get("identity_zone_id"));
    }
    
    @Test(expected=IdpAlreadyExistsException.class)
    public void testCreateIdentityProviderWithNonUniqueOriginKeyInDefaultZone() throws Exception {
        String originKey = RandomStringUtils.randomAlphabetic(6);
        IdentityProvider idp = MultitenancyFixture.identityProvider(originKey);
        db.create(idp);
        db.create(idp);
    }
    
    @Test(expected=IdpAlreadyExistsException.class)
    public void testCreateIdentityProviderWithNonUniqueOriginKeyInOtherZone() throws Exception {
        IdentityZoneHolder.set(MultitenancyFixture.identityZone(UUID.randomUUID().toString(),"myzone"));
        String originKey = RandomStringUtils.randomAlphabetic(6);
        IdentityProvider idp = MultitenancyFixture.identityProvider(originKey);
        db.create(idp);
        db.create(idp);
    }
    
    @Test
    public void testCreateIdentityProvidersWithSameOriginKeyInBothZones() throws Exception {
        String originKey = RandomStringUtils.randomAlphabetic(6);
        IdentityProvider idp = MultitenancyFixture.identityProvider(originKey);
        db.create(idp);
        IdentityZoneHolder.set(MultitenancyFixture.identityZone(UUID.randomUUID().toString(),"myzone"));
        db.create(idp);
    }
    
    @Test
    public void testRetrieveIdentityProviderById() {
        String originKey = RandomStringUtils.randomAlphabetic(6);
        String identityZoneId = RandomStringUtils.randomAlphabetic(6);
        String idpId = RandomStringUtils.randomAlphabetic(6);
        IdentityProvider idp = MultitenancyFixture.identityProvider(originKey);
        idp.setId(idpId);
        int rowsAdded = jdbcTemplate.update(JdbcIdentityProviderProvisioning.CREATE_IDENTITY_PROVIDER_SQL, idp.getId(), idp.getVersion(), idp.getCreated(), idp.getLastModified(), idp.getName(), idp.getOriginKey(), idp.getType(), idp.getConfig(), identityZoneId);
        assertEquals(1, rowsAdded);
        
        IdentityProvider retrievedIdp = db.retrieve(idpId);
        assertEquals(idp.getId(), retrievedIdp.getId());
        assertEquals(idp.getConfig(), retrievedIdp.getConfig());
        assertEquals(idp.getName(), retrievedIdp.getName());
        assertEquals(idp.getOriginKey(), retrievedIdp.getOriginKey());
    }
    
    @Test
    public void testRetrieveIdentityProviderByOriginInSameZone() {
        String originKey = RandomStringUtils.randomAlphabetic(6);
        String identityZoneId = RandomStringUtils.randomAlphabetic(6);
        String idpId = RandomStringUtils.randomAlphabetic(6);
        IdentityZone identityZone = MultitenancyFixture.identityZone(identityZoneId, "myzone");
        IdentityZoneHolder.set(identityZone);
        IdentityProvider idp = MultitenancyFixture.identityProvider(originKey);
        idp.setId(idpId);
        int rowsAdded = jdbcTemplate.update(JdbcIdentityProviderProvisioning.CREATE_IDENTITY_PROVIDER_SQL, idp.getId(), idp.getVersion(), idp.getCreated(), idp.getLastModified(), idp.getName(), idp.getOriginKey(), idp.getType(), idp.getConfig(), identityZoneId);
        assertEquals(1, rowsAdded);
        
        IdentityProvider retrievedIdp = db.retrieveByOrigin(idp.getOriginKey());
        assertEquals(idp.getId(), retrievedIdp.getId());
        assertEquals(idp.getConfig(), retrievedIdp.getConfig());
        assertEquals(idp.getName(), retrievedIdp.getName());
        assertEquals(idp.getOriginKey(), retrievedIdp.getOriginKey());
    }
    
    @Test(expected = EmptyResultDataAccessException.class)
    public void testRetrieveIdentityProviderByOriginInDifferentZone() {
        String originKey = RandomStringUtils.randomAlphabetic(6);
        String identityZoneId1 = RandomStringUtils.randomAlphabetic(6);
        String identityZoneId2 = RandomStringUtils.randomAlphabetic(6);
        String idpId = RandomStringUtils.randomAlphabetic(6);
        IdentityZone identityZone1 = MultitenancyFixture.identityZone(identityZoneId1, "myzone1");
        IdentityZone identityZone2 = MultitenancyFixture.identityZone(identityZoneId2, "myzone2");
        IdentityZoneHolder.set(identityZone1);
        IdentityProvider idp = MultitenancyFixture.identityProvider(originKey);
        idp.setId(idpId);
        int rowsAdded = jdbcTemplate.update(JdbcIdentityProviderProvisioning.CREATE_IDENTITY_PROVIDER_SQL, idp.getId(), idp.getVersion(), idp.getCreated(), idp.getLastModified(), idp.getName(), idp.getOriginKey(), idp.getType(), idp.getConfig(), identityZoneId1);
        assertEquals(1, rowsAdded);

        IdentityZoneHolder.set(identityZone2);
        db.retrieveByOrigin(idp.getOriginKey());
    }
}
