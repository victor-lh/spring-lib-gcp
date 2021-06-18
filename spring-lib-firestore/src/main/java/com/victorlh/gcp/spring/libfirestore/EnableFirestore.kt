package com.victorlh.gcp.spring.libfirestore

import com.victorlh.gcp.spring.libfirestore.conf.SpringFirestoreConf
import org.springframework.context.annotation.Import

@Import(SpringFirestoreConf::class)
annotation class EnableFirestore