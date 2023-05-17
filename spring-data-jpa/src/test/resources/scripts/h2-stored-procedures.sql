/;
DROP alias IF EXISTS plus1inout
/;
CREATE alias plus1inout AS $$
Integer plus1inout(Integer arg) {
    return arg + 1;
}
$$
/;
