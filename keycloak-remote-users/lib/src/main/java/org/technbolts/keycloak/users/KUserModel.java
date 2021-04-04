package org.technbolts.keycloak.users;

import org.jboss.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModelDefaultMethods;
import org.keycloak.models.utils.DefaultRoles;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class KUserModel extends UserModelDefaultMethods {

    private static final Logger LOG = Logger.getLogger(KUserModel.class);
    //
    private static final String FEDERATION_LINK = "federationLink";
    private static final String REQUIRED_ACTIONS = "requiredActions";
    private static final String SERVICE_ACCOUNT_CLIENT_LINK = "serviceAccountClientLink";
    private static final Set<String> FILTERED_KEYS =
            new HashSet<>(asList(REQUIRED_ACTIONS, FEDERATION_LINK, SERVICE_ACCOUNT_CLIENT_LINK));
    private static final Set<String> SINGLE_ATTRIBUTES =
            new HashSet<>(asList(EMAIL, FIRST_NAME, LAST_NAME, USERNAME));

    //
    private final KeycloakSession session;
    private final RealmModel realm;
    private final ComponentModel storageProviderModel;
    //
    private Instant createdAt;
    private Instant updatedAt;
    private final long id;
    private String username;
    private final JSONObject data;
    private boolean modified = false;

    public KUserModel(KeycloakSession ksession,
                      RealmModel realm,
                      ComponentModel storageProviderModel,
                      long id,
                      Instant createdAt,
                      Instant updatedAt,
                      String username,
                      JSONObject data) {
        this.session = ksession;
        this.realm = realm;
        this.storageProviderModel = storageProviderModel;
        //
        this.id = id;
        this.username = username;
        this.data = data;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public JSONObject data() {
        // Single Attribute cleanup
        for (String key : new HashSet<>(data.keySet())) {
            if (SINGLE_ATTRIBUTES.contains(key)) {
                Object o = data.opt(key);
                if (o instanceof JSONArray) {
                    JSONArray array = (JSONArray) o;
                    if (array.isEmpty()) {
                        data.remove(key);
                    } else {
                        data.put(key, array.get(0));
                    }
                }
            }
        }
        return data;
    }

    public long id() {
        return id;
    }

    /**
     * Defaults to 'f:' + storageProvider.getId() + ':' + getUsername()
     */
    @Override
    public String getId() {
        return new StorageId(storageProviderModel.getId(), getUsername()).getId();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        if (!Objects.equals(this.username, username))
            markModifed();
        this.username = username;
    }

    @Override
    public boolean isEmailVerified() {
        return data.optBoolean(EMAIL_VERIFIED, false);
    }

    @Override
    public void setEmailVerified(boolean verified) {
        data.put(EMAIL_VERIFIED, verified);
    }

    @Override
    public Long getCreatedTimestamp() {
        return createdAt != null ? createdAt.toEpochMilli() : null;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        LOG.infof("Updating CreatedTimestamp '%s'", timestamp);
        this.createdAt = Instant.ofEpochMilli(timestamp);
    }

    @Override
    public boolean isEnabled() {
        if (data.has(ENABLED))
            return data.getBoolean(ENABLED);
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        LOG.infof("Updating enabled '%s'", enabled);
        this.data.put(ENABLED, enabled);
        markModifed();
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        LOG.infof("Updating single attribute '%s', '%s'", name, value);
        if (USERNAME.equals(name)) {
            setUsername(name);
        } else {
            this.data.put(name, value);
            markModifed();
        }
    }

    @Override
    public String getFirstAttribute(String name) {
        if (USERNAME.equals(name))
            return getUsername();

        Object o = this.data.opt(name);
        if (o == null)
            return null;
        if (o instanceof JSONArray) {
            JSONArray array = (JSONArray) o;
            if (array.isEmpty())
                return null;
            return array.getString(0);
        }
        return o.toString();
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        LOG.infof("Updating attribute '%s', '%s'", name, values);

        if (USERNAME.equals(name)) {
            setUsername(values.get(0));
        } else {
            data.put(name, values);
            markModifed();
        }
    }

    @Override
    public List<String> getAttribute(String name) {
        if (USERNAME.equals(name))
            return Collections.singletonList(getUsername());

        Object o = this.data.opt(name);
        if (o == null)
            return Collections.emptyList();
        if (o instanceof JSONArray) {
            JSONArray array = (JSONArray) o;
            if (array.isEmpty())
                return null;
            return array.toList().stream().map(Object::toString).collect(toList());
        }
        return Collections.singletonList(o.toString());
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = new HashMap<>();
        for (String key : data.keySet()) {
            if (!FILTERED_KEYS.contains(key))
                attrs.put(key, getAttribute(key));
        }
        attrs.put(USERNAME, Collections.singletonList(getUsername()));
        return attrs;
    }

    @Override
    public void removeAttribute(String name) {
        LOG.infof("Removing attribute '%s'", name);
        if (data.remove(name) != null) {
            markModifed();
        }
    }

    @Override
    public Set<String> getRequiredActions() {
        Set<String> ret = new HashSet<>();
        if (data.has(REQUIRED_ACTIONS)) {
            JSONArray array = data.getJSONArray(REQUIRED_ACTIONS);
            for (Object o : array) {
                ret.add(o.toString());
            }
        }
        return ret;
    }

    @Override
    public void addRequiredAction(String action) {
        LOG.infof("Add required action '%s'", action);
        JSONArray array = data.optJSONArray(REQUIRED_ACTIONS);
        data.append(REQUIRED_ACTIONS, action);
        markModifed();
    }

    @Override
    public void removeRequiredAction(String action) {
        LOG.infof("Remove required action '%s'", action);
        if (data.has(REQUIRED_ACTIONS)) {
            Iterator<Object> iterator = data.getJSONArray(REQUIRED_ACTIONS).iterator();
            while (iterator.hasNext()) {
                Object o = iterator.next();
                if (o.equals(action)) {
                    iterator.remove();
                    markModifed();
                }
            }
        }
    }

    /**
     * Get group membership mappings that are managed by this storage provider
     */
    protected Set<GroupModel> getGroupsInternal() {
        return Collections.emptySet();
    }

    /**
     * Should the realm's default groups be appended to getGroups() call?
     * If your storage provider is not managing group mappings then it is recommended that
     * this method return true
     */
    protected boolean appendDefaultGroups() {
        return true;
    }

    @Override
    public Set<GroupModel> getGroups() {
        Set<GroupModel> set = new HashSet<>();
        if (appendDefaultGroups()) set.addAll(realm.getDefaultGroupsStream().collect(Collectors.toSet()));
        set.addAll(getGroupsInternal());
        return set;
    }

    @Override
    public void joinGroup(GroupModel group) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public void leaveGroup(GroupModel group) {
        throw new ReadOnlyException("user is read only for this update");

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return RoleUtils.isMember(getGroups().stream(), group);
    }

    @Override
    public String getFederationLink() {
        return data.optString(FEDERATION_LINK, null);
    }

    @Override
    public void setFederationLink(String link) {
        data.put(FEDERATION_LINK, link);
        markModifed();
    }

    @Override
    public String getServiceAccountClientLink() {
        return data.optString(SERVICE_ACCOUNT_CLIENT_LINK, null);
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        data.put(SERVICE_ACCOUNT_CLIENT_LINK, clientInternalId);
        markModifed();
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        return getRoleMappings().stream().filter(RoleUtils::isRealmRole).collect(Collectors.toSet());
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        return getRoleMappings().stream().filter(r -> RoleUtils.isClientRole(r, app)).collect(Collectors.toSet());
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return RoleUtils.hasRole(getRoleMappings().stream(), role)
                || RoleUtils.hasRoleFromGroup(getGroups().stream(), role, true);
    }

    @Override
    public void grantRole(RoleModel role) {
        throw new ReadOnlyException("user is read only for this update");
    }

    /**
     * Should the realm's default roles be appended to getRoleMappings() call?
     * If your storage provider is not managing all role mappings then it is recommended that
     * this method return true
     *
     * @return
     */
    protected boolean appendDefaultRolesToRoleMappings() {
        return true;
    }

    protected Set<RoleModel> getRoleMappingsInternal() {
        return Collections.emptySet();
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        Set<RoleModel> set = new HashSet<>();
        if (appendDefaultRolesToRoleMappings())
            set.addAll(DefaultRoles.getDefaultRoles(realm).collect(Collectors.toSet()));
        set.addAll(getRoleMappingsInternal());
        return set;
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        throw new ReadOnlyException("user is read only for this update");
    }

    private void markModifed() {
        modified = true;
    }

    public boolean isModified() {
        return modified;
    }
}
