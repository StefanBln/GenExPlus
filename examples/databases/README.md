# Database configuration examples

Copy-paste recipes for multi-database deployments. See [docs/DATABASE.md](../../docs/DATABASE.md) for the full guide.

## Files

| File | Purpose |
|------|---------|
| `application.properties` | Two JDBC profiles: `db1` (PostgreSQL), `db2` (MySQL) |
| `db1.jrdax` | Studio JDBC adapter export for `db1` (optional; `db1.*` in properties wins) |
| `report-warehouse.conf` | Job using `database.id=db1` |
| `report-crm.conf` | Job using `database.id=db2` |
| `templates/warehouse-summary.jrxml` | Studio `defaultdataadapter=db1` |
| `templates/crm-pipeline.jrxml` | Studio `defaultdataadapter=db2` |

## Local test databases

```bash
./scripts/test-db-up.sh
cp application.properties /path/to/your/application.properties
# Edit report.output.dir in the job files, then:
../../start.sh report-warehouse.conf
```

## Alignment checklist

- [ ] Studio data adapter name matches `defaultdataadapter` in JRXML (and `database.id` if you set it)
- [ ] `dbN` prefix in `application.properties` matches the adapter name
- [ ] Export `dbN.jrdax` from Studio for local defaults (optional)
- [ ] Production credentials live in `application.properties` or `REPORT_DBN_*` env vars
