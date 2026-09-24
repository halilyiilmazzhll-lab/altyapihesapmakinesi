import codecs
import re

# 1. Update AppDatabase.kt
path = 'app/src/main/java/com/example/egimhesabi/data/AppDatabase.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    db_content = f.read()

db_content = db_content.replace('version = 14,', 'version = 15,')
db_content = db_content.replace('MIGRATION_13_14)', 'MIGRATION_13_14, MIGRATION_14_15)')

migration_14_15 = '''
        internal val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE stakeout_manholes ADD COLUMN connectedToNameKey TEXT DEFAULT NULL")
            }
        }
    }
}'''
db_content = db_content.replace('    }\n}', migration_14_15)

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(db_content)


# 2. Update StakeoutManholeEntity.kt
path = 'app/src/main/java/com/example/egimhesabi/data/StakeoutManholeEntity.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    entity_content = f.read()

entity_content = entity_content.replace('val sourceFileName: String = "",', 'val connectedToNameKey: String? = null,\n    val sourceFileName: String = "",')

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(entity_content)


# 3. Update StakeoutDao.kt
path = 'app/src/main/java/com/example/egimhesabi/data/StakeoutDao.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    dao_content = f.read()

update_func = '''
    @Query("UPDATE stakeout_manholes SET connectedToNameKey = :targetNameKey, updatedAt = :time WHERE id = :id")
    suspend fun updateConnection(id: Long, targetNameKey: String?, time: Long = System.currentTimeMillis())
}'''
# Find the last closing brace
last_brace_idx = dao_content.rfind('}')
if last_brace_idx != -1:
    dao_content = dao_content[:last_brace_idx] + update_func + dao_content[last_brace_idx+1:]
    with codecs.open(path, 'w', 'utf-8') as f:
        f.write(dao_content)


# 4. Update StakeoutViewModel.kt
path = 'app/src/main/java/com/example/egimhesabi/viewmodel/StakeoutViewModel.kt'
with codecs.open(path, 'r', 'utf-8') as f:
    vm_content = f.read()

vm_func = '''
    fun updateConnection(manholeId: Long, targetNameKey: String?) {
        viewModelScope.launch {
            try {
                db.stakeoutDao().updateConnection(manholeId, targetNameKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}'''
last_brace_idx = vm_content.rfind('}')
if last_brace_idx != -1:
    vm_content = vm_content[:last_brace_idx] + vm_func + vm_content[last_brace_idx+1:]
    with codecs.open(path, 'w', 'utf-8') as f:
        f.write(vm_content)

print("DB and VM patched successfully")