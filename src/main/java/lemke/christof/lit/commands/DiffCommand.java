import lemke.christof.lit.database.Blob;
import lemke.christof.lit.database.Oid;
    private record Target(Path path, Oid oid, String mode, String data) {
    private static final Oid NULL_OID = Oid.of(Util.repeat("0", 40));
        Oid oid = blob.oid();
        Oid oid = entry.oid();
        String oidRange = "index " + a.oid.shortOid() + ".." + b.oid.shortOid();