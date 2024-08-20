package com.wjx.kablade.AllWeapon.awlib;

public class ArrayLib {
    public static Class[][] RotateClockwise(Class[][] arry1, int m, int n) {  Class[][] arry2 = new Class[n][m]; int dst; dst = m - 1; for (int x = 0; x < m; x++, dst--) { for (int y = 0; y < n; y++) { arry2[y][dst] = arry1[x][y]; } } return arry2; }
}
