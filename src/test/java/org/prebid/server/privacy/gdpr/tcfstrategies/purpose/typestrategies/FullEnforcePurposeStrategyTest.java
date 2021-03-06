package org.prebid.server.privacy.gdpr.tcfstrategies.purpose.typestrategies;

import com.iabtcf.decoder.TCString;
import com.iabtcf.utils.IntIterable;
import com.iabtcf.v2.PublisherRestriction;
import com.iabtcf.v2.RestrictionType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.prebid.server.privacy.gdpr.model.PrivacyEnforcementAction;
import org.prebid.server.privacy.gdpr.model.VendorPermission;
import org.prebid.server.privacy.gdpr.model.VendorPermissionWithGvl;
import org.prebid.server.privacy.gdpr.vendorlist.proto.VendorV2;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FullEnforcePurposeStrategyTest {
    private static final int PURPOSE_ID = 1;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private FullEnforcePurposeStrategy target;

    @Mock
    private TCString tcString;
    @Mock
    private IntIterable allowedVendors;
    @Mock
    private IntIterable allowedVendorsLI;
    @Mock
    private IntIterable purposesConsent;
    @Mock
    private IntIterable purposesLI;

    @Mock
    private PublisherRestriction publisherRestriction;

    @Before
    public void setUp() {
        given(tcString.getVendorConsent()).willReturn(allowedVendors);
        given(tcString.getVendorLegitimateInterest()).willReturn(allowedVendorsLI);
        given(tcString.getPurposesConsent()).willReturn(purposesConsent);
        given(tcString.getPurposesLITransparency()).willReturn(purposesLI);

        given(tcString.getPublisherRestrictions()).willReturn(singletonList(publisherRestriction));

        given(publisherRestriction.getPurposeId()).willReturn(PURPOSE_ID);
        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.UNDEFINED);

        given(allowedVendors.contains(anyInt())).willReturn(false);
        given(allowedVendorsLI.contains(anyInt())).willReturn(false);
        given(purposesConsent.contains(anyInt())).willReturn(false);
        given(purposesLI.contains(anyInt())).willReturn(false);

        target = new FullEnforcePurposeStrategy();
    }

    @Test
    public void shouldEmptyWhenPublisherRestrictionIsZeroAndExcludedBidderPresent() {
        // given
        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.NOT_ALLOWED);

        final VendorPermission vendorPermission1 = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermission vendorPermission2 = VendorPermission.of(2, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWithGvl1 = VendorPermissionWithGvl.of(vendorPermission1,
                VendorV2.empty(1));
        final VendorPermissionWithGvl vendorPermissionWithGvl2 = VendorPermissionWithGvl.of(vendorPermission2,
                VendorV2.empty(2));

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                singletonList(vendorPermissionWithGvl1), singletonList(vendorPermissionWithGvl2), false);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    public void shouldAllowWhenInGvlPurposeAndPurposeConsentAllowed() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(emptySet())
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(purposesConsent.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesConsent).contains(PURPOSE_ID);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeLIAllowed() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(emptySet())
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(purposesLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesLI);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeAndPurposeConsentAllowedAndVendorConsentAllowedAndEnforced() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(emptySet())
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesConsent).contains(PURPOSE_ID);
        verify(allowedVendors).contains(1);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeConsentAllowedAndVendorLIAllowedAndEnforced() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(emptySet())
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(purposesConsent).contains(PURPOSE_ID);
        verifyZeroInteractions(purposesLI);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeLIAndPurposeLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(emptySet())
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(purposesLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesLI).contains(PURPOSE_ID);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeLIAndPurposeLIAndVendorLIAllowedAndEnforced() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(emptySet())
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesLI).contains(PURPOSE_ID);
        verify(allowedVendorsLI).contains(1);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeConsentAllowedAndVendorConsentAllowedAndEnforced() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(emptySet())
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(purposesLI).contains(PURPOSE_ID);
        verifyZeroInteractions(allowedVendors);
    }

    // Flexible GVL Purpose part

    // Restriction type is REQUIRE_CONSENT

    @Test
    public void shouldAllowWhenInGvlPurposeAndPurposeConsentAllowedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesConsent.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesConsent).contains(PURPOSE_ID);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeAndPurposeConsentAndVendorConsentAndEnforcedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesConsent).contains(PURPOSE_ID);
        verify(allowedVendors).contains(1);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeLIAllowedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesLI);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeLIAndVendorLIAllowedAndEnforcedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesLI);
        verifyZeroInteractions(allowedVendorsLI);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeConsentAndVendorLIAndEnforcedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(purposesConsent).contains(PURPOSE_ID);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeLIAndVendorConsentAndEnforcedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();
    }

    // Restriction tipe is REQUIRE_LEGITIMATE_INTEREST

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeConsentAllowedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesConsent.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesConsent);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeConsentAndVendorConsentAndEnforcedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesConsent);
        verifyZeroInteractions(allowedVendors);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeAndPurposeLIAllowedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesLI).contains(PURPOSE_ID);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeAndPurposeLIAndVendorLIAllowedAndEnforcedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesLI).contains(PURPOSE_ID);
        verify(allowedVendorsLI).contains(1);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeConsentAndVendorLIAllowedAndEnforcedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(allowedVendorsLI).contains(1);
        verifyZeroInteractions(purposesConsent);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeAndPurposeLIAndVendorConsentAllowedAndEnforcedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .purposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(purposesLI).contains(PURPOSE_ID);
        verifyZeroInteractions(allowedVendors);
    }

    // Flexible GVL Purpose Legitimate interest part

    // Restriction type is REQUIRE_CONSENT

    @Test
    public void shouldAllowWhenInGvlPurposeLIAndPurposeConsentAllowedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesConsent.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesConsent).contains(PURPOSE_ID);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeLIAndPurposeAndVendorConsentAndEnforcedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesConsent).contains(PURPOSE_ID);
        verify(allowedVendors).contains(1);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeLIAllowedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesLI);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeLIAndVendorLIAllowedAndEnforcedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesLI);
        verifyZeroInteractions(allowedVendorsLI);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeConsentAndVendorLIAndEnforcedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(purposesConsent).contains(PURPOSE_ID);
        verifyZeroInteractions(allowedVendorsLI);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeLIAndVendorConsentAndEnforcedAndFlexibleAndRequireConsent() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_CONSENT);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(allowedVendors).contains(PURPOSE_ID);
        verifyZeroInteractions(purposesLI);
    }

    // Restriction type is REQUIRE_LEGITIMATE_INTEREST

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeConsentAllowedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesConsent.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesConsent);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeConsentAndVendorConsentAndEnforcedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verifyZeroInteractions(purposesConsent);
        verifyZeroInteractions(allowedVendors);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeLIAndPurposeLIAllowedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), false);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesLI).contains(PURPOSE_ID);
    }

    @Test
    public void shouldAllowWhenInGvlPurposeLIAndPurposeLIAndVendorLIAllowedAndEnforcedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission);

        verify(purposesLI).contains(PURPOSE_ID);
        verify(allowedVendorsLI).contains(1);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeConsentAndVendorLIAllowedAndEnforcedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesConsent.contains(anyInt())).willReturn(true);
        given(allowedVendorsLI.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(allowedVendorsLI).contains(1);
        verifyZeroInteractions(purposesConsent);
    }

    @Test
    public void shouldEmptyWhenInGvlPurposeLIAndPurposeLIAndVendorConsentAllowedAndEnforcedAndFlexibleAndRequireLI() {
        // given
        final VendorV2 vendorGvl = VendorV2.builder()
                .legIntPurposes(singleton(PURPOSE_ID))
                .flexiblePurposes(singleton(PURPOSE_ID))
                .build();

        final VendorPermission vendorPermission = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl = VendorPermissionWithGvl.of(vendorPermission, vendorGvl);
        final List<VendorPermissionWithGvl> vendorPermissionWithGvls = singletonList(vendorPermissionWitGvl);

        given(publisherRestriction.getRestrictionType()).willReturn(RestrictionType.REQUIRE_LEGITIMATE_INTEREST);

        given(purposesLI.contains(anyInt())).willReturn(true);
        given(allowedVendors.contains(anyInt())).willReturn(true);

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                vendorPermissionWithGvls, emptyList(), true);

        // then
        assertThat(result).isEmpty();

        verify(purposesLI).contains(1);
        verifyZeroInteractions(allowedVendors);
    }

    @Test
    public void shouldReturnExcludedVendors() {
        // given
        final VendorPermission vendorPermission1 = VendorPermission.of(1, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermission vendorPermission2 = VendorPermission.of(2, null, PrivacyEnforcementAction.restrictAll());
        final VendorPermissionWithGvl vendorPermissionWitGvl1 = VendorPermissionWithGvl.of(vendorPermission1,
                VendorV2.empty(1));
        final VendorPermissionWithGvl vendorPermissionWitGvl2 = VendorPermissionWithGvl.of(vendorPermission2,
                VendorV2.empty(2));

        // when
        final Collection<VendorPermission> result = target.allowedByTypeStrategy(PURPOSE_ID, tcString,
                singleton(vendorPermissionWitGvl1), singleton(vendorPermissionWitGvl2), true);

        // then
        assertThat(result).usingFieldByFieldElementComparator().containsOnly(vendorPermission2);
    }
}

