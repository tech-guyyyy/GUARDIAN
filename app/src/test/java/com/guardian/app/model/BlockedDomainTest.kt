package com.guardian.app.model

import com.google.common.truth.Truth.assertThat
import com.guardian.app.core.model.BlockedDomain
import org.junit.Test

class BlockedDomainTest {

    @Test
    fun `normalize strips protocol, www, trailing paths, and ports`() {
        assertThat(BlockedDomain.normalize("https://www.example.com/login?param=1")).isEqualTo("example.com")
        assertThat(BlockedDomain.normalize("http://bad-site.org/videos/adult/")).isEqualTo("bad-site.org")
        assertThat(BlockedDomain.normalize("SUBDOMAIN.Domain.COM:8080/path")).isEqualTo("subdomain.domain.com")
        assertThat(BlockedDomain.normalize("  WWW.TEST.CO.UK  ")).isEqualTo("test.co.uk")
    }
}
