package scripts.databaseApproach

import groovy.util.logging.Slf4j

@Slf4j
class DomainClass {
    String name
    int propx = 10
    String propStr = "William"

    String toString() {
        def id
        if (hasProperty('id'))
            id = getProperty ('id')

        if (id)
            "DomainClass (id: $id, name: [$name])"
        else
            "DomainClass (name: [$name])"
    }
}
