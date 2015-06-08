package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 解多元一次方程组 只能解决n个变量n个方程的情况
 */
public class Equation {
	private static final BigDecimal ZERO = new BigDecimal("0");
	private static final BigDecimal ONE = new BigDecimal("1");

	public static void main(String[] args) {
		/**
		 * 方程组 x + y = 50 0.8*x + 1.2*y = 60
		 */
		BigDecimal[][] matrix = new BigDecimal[][] {
				{ new BigDecimal("1"), new BigDecimal("1"),
						new BigDecimal("50") },
				{ new BigDecimal("0.8"), new BigDecimal("1.2"),
						new BigDecimal("60") } };
		BigDecimal[] rst = new Equation().solveEquation(matrix, 3,
				RoundingMode.HALF_UP);
		for (int i = 0; i < rst.length; ++i) {
			System.out.println(rst[i]);
		}
	}

	/**
	 * 解多元一次方程组 只能解决n个变量n个方程的情况,即矩阵是n*(n+1)的形式
	 * 
	 * @param matrix
	 *            矩阵
	 * @param scale
	 *            精确小数位数
	 * @param roundingMode
	 *            舍入模式
	 * @return
	 */
	public BigDecimal[] solveEquation(BigDecimal[][] matrix, int scale,
			RoundingMode roundingMode) {
		if (isNullOrEmptyMatrix(matrix)) {
			return new BigDecimal[0];
		}
		BigDecimal[][] triangular = elimination(matrix, scale, roundingMode);
		return substitutionUpMethod(triangular, scale, roundingMode);
	}

	/**
	 * 用高斯消元法将矩阵变为上三角形矩阵
	 *
	 * @param matrix
	 * @param scale
	 *            精确小数位数
	 * @param roundingMode
	 *            舍入模式
	 * @return
	 */
	private BigDecimal[][] elimination(BigDecimal[][] matrix, int scale,
			RoundingMode roundingMode) {
		if (isNullOrEmptyMatrix(matrix)
				|| matrix.length != matrix[0].length - 1) {
			return new BigDecimal[0][0];
		}
		int matrixLine = matrix.length;
		for (int i = 0; i < matrixLine - 1; ++i) {
			// 第j行的数据 - (第i行的数据 / matrix[i][i])*matrix[j][i]
			for (int j = i + 1; j < matrixLine; ++j) {
				for (int k = i + 1; k <= matrixLine; ++k) {
					// matrix[j][k] = matrix[j][k] -
					// (matrix[i][k]/matrix[i][i])*matrix[j][i];
					matrix[j][k] = matrix[j][k].subtract((matrix[i][k].divide(
							matrix[i][i], scale, roundingMode)
							.multiply(matrix[j][i])));
				}
				matrix[j][i] = ZERO;
			}
		}
		return matrix;
	}

	/**
	 * 回代求解(针对上三角形矩阵)
	 *
	 * @param matrix
	 *            上三角阵
	 * @param scale
	 *            精确小数位数
	 * @param roundingMode
	 *            舍入模式
	 */
	private BigDecimal[] substitutionUpMethod(BigDecimal[][] matrix, int scale,
			RoundingMode roundingMode) {
		int row = matrix.length;
		for (int i = 0; i < row; ++i) {
			if (matrix[i][i].equals(ZERO.setScale(scale))) {// 方程无解或者解不惟一
				return new BigDecimal[0];
			}
		}
		BigDecimal[] result = new BigDecimal[row];
		for (int i = 0; i < result.length; ++i) {
			result[i] = ONE;
		}
		BigDecimal tmp;
		for (int i = row - 1; i >= 0; --i) {
			tmp = ZERO;
			int j = row - 1;
			while (j > i) {
				tmp = tmp.add(matrix[i][j].multiply(result[j]));
				--j;
			}
			result[i] = matrix[i][row].subtract(tmp).divide(matrix[i][i],
					scale, roundingMode);
		}
		return result;
	}

	/**
	 * 判断系数矩阵是否是null或空数组
	 * 
	 * @param matrix
	 *            系数矩阵
	 * @return null或空数组返回true,否则返回false
	 */
	private static boolean isNullOrEmptyMatrix(BigDecimal[][] matrix) {
		if (matrix == null || matrix.length == 0) {
			return true;
		}
		int row = matrix.length;
		int col = matrix[0].length;
		for (int i = 0; i < row; ++i) {
			for (int j = 0; j < col; ++j) {
				if (matrix[i][j] == null) {
					return true;
				}
			}
		}
		return false;
	}
}
