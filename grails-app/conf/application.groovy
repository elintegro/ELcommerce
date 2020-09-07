grails.controllers.upload.maxFileSize=5000000
grails.controllers.upload.maxRequestSize=5000000

// Added by the Spring Security Core plugin:
grails.plugin.springsecurity.shiro.active                      = true
grails.plugin.springsecurity.logout.postOnly                   = false
grails.plugin.springsecurity.userLookup.userDomainClassName    = 'io.hilo.Account'
grails.plugin.springsecurity.userLookup.authorityJoinClassName = 'io.hilo.AccountRole'
grails.plugin.springsecurity.authority.className               = 'io.hilo.Role'
grails.plugin.springsecurity.shiro.permissionDomainClassName   = 'io.hilo.Permission'

grails.plugin.springsecurity.controllerAnnotations.staticRules = [
	[pattern: '/',               access: ['permitAll']],
	[pattern: '/error',          access: ['permitAll']],
	[pattern: '/notFound',       access: ['permitAll']],
	[pattern: '/index',          access: ['permitAll']],
	[pattern: '/index.gsp',      access: ['permitAll']],
	[pattern: '/shutdown',       access: ['permitAll']],
	[pattern: '/assets/**',      access: ['permitAll']],
	[pattern: '/uploads/**',     access: ['permitAll']],
	[pattern: '/**/js/**',       access: ['permitAll']],
	[pattern: '/**/css/**',      access: ['permitAll']],
	[pattern: '/**/images/**',   access: ['permitAll']],
	[pattern: '/**/fonts/**',    access: ['permitAll']],
	[pattern: '/**/favicon.ico', access: ['permitAll']],
	[pattern: '/elintegro_ELcommerce/userDetailsFromElintegro', access: ['permitAll']],
	[pattern: '/elintegro_ELcommerce/authenticateWithToken', access: ['permitAll']]

]

grails.plugin.springsecurity.filterChain.chainMap = [
	[pattern: '/assets/**',      filters: 'none'],
	[pattern: '/**/js/**',       filters: 'none'],
	[pattern: '/**/css/**',      filters: 'none'],
	[pattern: '/**/images/**',   filters: 'none'],
	[pattern: '/**/fonts',       filters: 'none'],
	[pattern: '/**/favicon.ico', filters: 'none'],
	[pattern: '/**',             filters: 'JOINED_FILTERS']
]


//grails.plugin.springsecurity.successHandler.alwaysUseDefault = true
//grails.plugin.springsecurity.successHandler.defaultTargetUrl = '/admin'


dataSource {
	pooled = false
	driverClassName = "com.mysql.cj.jdbc.Driver"
	dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
	jmxExport = true
//	username = "developer"
//	password = "java11"
}

environments {
	jdbc:
	mysql:
	development {
		server.contextPath = "/hilo"
		rootPath = ""
		server.port = 8098
		grails.serverURL = "http://localhost:${server.port}"
		grails.plugin.springsecurity.ui.register.emailFrom = 'elintegro@localhost'
		dataSource {
			logSql = true
			dbCreate = 'create-drop' //"update" // one of 'create', 'create-drop','update'
//			dbCreate = 'update' //"update" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost:3306/elintegro_ecommerce_db_dev"
//			username = "root"
//			password = "qbohfoj"
			username = "developer"
			password = "java11"
		}
	}
	production {
		server.contextPath = "/"
		rootPath = ""
		grails.serverURL = ""
		grails.plugin.springsecurity.ui.register.emailFrom='elintegro.himalaya'
		dataSource {
			logSql = true
//			dbCreate = 'create-drop' //"update" // one of 'create', 'create-drop','update'
			dbCreate = 'update' //"update" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost:3306/elintegro_website_db_dev?useUnicode=true&characterEncoding=UTF-8"
			username = "developer"
			password = "java1177"

			logSql = true

			properties {
				jmxEnabled = true
				initialSize = 5
				maxActive = 50
				minIdle = 5
				maxIdle = 25
				maxWait = 10000
				maxAge = 10 * 60000
				timeBetweenEvictionRunsMillis = 5000
				minEvictableIdleTimeMillis = 60000
				validationQuery = "SELECT 1"
				validationQueryTimeout = 3
				validationInterval = 15000
				testOnBorrow = true
				testWhileIdle = true
				testOnReturn = false
				jdbcInterceptors = "ConnectionState"
				defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
			}
		}
	}
}