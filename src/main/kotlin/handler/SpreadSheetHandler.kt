package handler

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.*
import config.GoogleSpreadSheetConfig
import kotlinx.html.InputType
import logger
import java.io.File
import java.util.*

class SpreadSheetHandler {
    companion object {
        val instance by lazy {
            SpreadSheetHandler()
        }
    }

    private val tableRange = "'${GoogleSpreadSheetConfig.sheetName}'!${GoogleSpreadSheetConfig.firstDataCell}:${GoogleSpreadSheetConfig.lastDataCell}"

    private var sheetService: Sheets? = null
    private var tableContent = mutableListOf<MutableList<Any>>()    // Major Dimension: Columns
    private var tableColor = listOf<List<Color>>()      // Major Dimension: Rows (I can't change that)

    private val googleCredentialsFilePath = "data\\tokens\\google_credentials.json"
    private val storedCredentialsTokenFolder = "data\\tokens"

    private val defaultColorRGBA = Color().setRed(1.0f).setGreen(1.0f).setBlue(1.0f).setAlpha(1.0f)

    fun setupConnectionAndLoadData(runNamesRedeemHandler: RunNamesRedeemHandler) {
        for(i in 0..1) {
            try {
                val jsonFactory = GsonFactory.getDefaultInstance()
                val clientSecrets = GoogleClientSecrets.load(jsonFactory, File(googleCredentialsFilePath).reader())
                val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

                val flow: GoogleAuthorizationCodeFlow = GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, jsonFactory, clientSecrets, Collections.singletonList(SheetsScopes.SPREADSHEETS)
                )
                    .setDataStoreFactory(FileDataStoreFactory(File(storedCredentialsTokenFolder)))
                    .setAccessType("offline")
                    .build()

                val receiver = LocalServerReceiver.Builder().setPort(8888).build()

                sheetService = Sheets.Builder(
                    httpTransport,
                    jsonFactory,
                    AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
                )
                    .setApplicationName("Sheet Service")
                    .build()

            } catch (e: Exception) {
                logger.error("An error occured while setting up connection to Google: ", e)
            }

            try {
                // dummy test to see if the credentials need to be refreshed
                sheetService?.spreadsheets()?.values()
                    ?.get(GoogleSpreadSheetConfig.spreadSheetId, "A1:A1")
                    ?.execute()
            } catch (e: Exception) {
                logger.warn("Check for sheetService failed. Deleting token and trying again...")
                File("$storedCredentialsTokenFolder\\StoredCredential").delete()
                continue
            }

            break
        }

        logger.info("Connected to Google Spreadsheet service")

        loadTableContentAndColor(runNamesRedeemHandler)
    }

    private fun loadTableContentAndColor(runNamesRedeemHandler: RunNamesRedeemHandler) {
        val sheetService = sheetService ?: run {
            logger.error("Sheet Service is not setup. Aborting leaderboard handling... ")
            return
        }

        try {
            val input = sheetService.spreadsheets().values()
                .get(GoogleSpreadSheetConfig.spreadSheetId, tableRange)
                .setMajorDimension("COLUMNS")
                .execute()
                .getValues() as MutableList<MutableList<Any>>

            val lastCellLetterIndex = transformLetterFromToIndex(GoogleSpreadSheetConfig.lastDataCell.filter { it.isLetter() }).toInt()
            var i = transformLetterFromToIndex(GoogleSpreadSheetConfig.firstDataCell.filter { it.isLetter() }).toInt()
            val startIndex = i
            val output = mutableListOf<MutableList<Any>>()
            while (i <= lastCellLetterIndex) {
                val currentCell =
                    "'${GoogleSpreadSheetConfig.sheetName}'!${transformLetterFromToIndex(i.toString())}${GoogleSpreadSheetConfig.firstDataCell.filter { it.isDigit() }}"
                val cellValue = sheetService.spreadsheets().values()
                    .get(GoogleSpreadSheetConfig.spreadSheetId, currentCell)
                    .setMajorDimension("COLUMNS")
                    .execute()
                    .getValues()

                try {
                    if (cellValue[0][0] != input[i - startIndex][0]) {
                        output.add(mutableListOf())
                    } else {
                        output.add(input[i - startIndex])
                    }
                } catch (e: Exception) {
                    output.add(mutableListOf())
                }

                i++
            }

            logger.info("Received data from spread sheet and fixed the input")
            tableContent = output


            val formatResult = sheetService.spreadsheets()
                .get(GoogleSpreadSheetConfig.spreadSheetId)
                .setRanges(mutableListOf(tableRange))
                .setIncludeGridData(true)
                .execute()

            tableColor = formatResult.sheets[0].data[0]
                .rowData.map { rowData ->
                    rowData.getValues()
                        .map { cellData ->
                            cellData.userEnteredFormat.backgroundColor
                        }
                }

            tableContent.forEachIndexed { columnIndex, column ->
                column.forEachIndexed { rowIndex, cell ->
                    if((cell as String) != "") {
                        runNamesRedeemHandler.saveNameWithColor(
                            cell.toString(),
                            tableColor[rowIndex][columnIndex].let {
                                try {
                                    Integer.toHexString((it.red * 255).toInt()).uppercase(Locale.getDefault())
                                } catch (_: Exception) {
                                    "00"
                                } +
                                try {
                                    Integer.toHexString((it.green * 255).toInt()).uppercase(Locale.getDefault())
                                } catch (_: Exception) {
                                    "00"
                                } +
                                try{
                                    Integer.toHexString((it.blue * 255).toInt()).uppercase(Locale.getDefault())
                                } catch (_: Exception) {
                                    "00"
                                }
                            }
                        )
                    }
                }
            }

            logger.info("Received color data from spread sheet")
        } catch (e: Exception) {
            logger.error("An error occurred while loading the initial table content. Setting table content to an empty list. ", e)
        }
    }

    private fun updateCellsBackgroundColor(oldRowIndex: Int, oldColumnIndex: Int, newRowIndex: Int, newColumnIndex: Int, hexColor: String) {
        // TODO Test as soon as the whole grid color update works
        val rgbaColor = Color()
            .setRed((hexColor.substring(0, 2).toInt(16).toFloat() / 255f))
            .setGreen((hexColor.substring(2, 4).toInt(16).toFloat() / 255f))
            .setBlue((hexColor.substring(4).toInt(16).toFloat() / 255f))
            .setAlpha(1.0f)


        val cellsWithNewColor = mutableMapOf<Pair</* row: */Int, /* column: */Int>, Color>()
        cellsWithNewColor[Pair(newRowIndex, newColumnIndex)] = rgbaColor
        if(oldRowIndex != -1) {
            val subList = mutableListOf<Color>()
            for(i in oldRowIndex + 1..tableContent[oldColumnIndex].filter { it != "" }.size) {
                subList.add(tableColor[i][oldColumnIndex])
            }

            subList.forEachIndexed { index, _ ->
                var color = subList[index]
                if(index == subList.lastIndex) {
                    color = defaultColorRGBA
                }
                cellsWithNewColor[Pair(index + oldRowIndex, oldColumnIndex)] = color
            }
        }
        
        val data = mutableListOf<RowData>()
        val newTableColor = mutableListOf<List<Color>>()
        tableColor.forEachIndexed { rowIndex, row ->
            val currentRow = mutableListOf<CellData>()
            val currentRowColor = mutableListOf<Color>()
            row.forEachIndexed { columnIndex, color ->
                val newColor = if(cellsWithNewColor.containsKey(Pair(rowIndex, columnIndex))) {
                    cellsWithNewColor[Pair(rowIndex, columnIndex)]
                } else {
                    color
                }

                currentRow.add(
                    CellData().setUserEnteredFormat(CellFormat().setBackgroundColor(newColor).setHorizontalAlignment("CENTER"))
                )
                currentRowColor.add(newColor ?: defaultColorRGBA)
            }
            data.add(RowData().setValues(currentRow))
            newTableColor.add(currentRowColor)
        }

        tableColor = newTableColor

        val startRowIndex = GoogleSpreadSheetConfig.firstDataCell.filter { it.isDigit() }.toInt() - 1
        val startColumnIndex = transformLetterFromToIndex(GoogleSpreadSheetConfig.firstDataCell.filter { it.isLetter() }).toInt()
        val gridRange = GridRange()
            .setEndColumnIndex(transformLetterFromToIndex(GoogleSpreadSheetConfig.lastDataCell.filter { it.isLetter() }).toInt() + startColumnIndex)
            .setEndRowIndex(GoogleSpreadSheetConfig.lastDataCell.filter { it.isDigit() }.toInt() - 1 + startRowIndex)
            .setSheetId(GoogleSpreadSheetConfig.sheetId)
            .setStartRowIndex(startRowIndex)
            .setStartColumnIndex(startColumnIndex)

        val requestList = mutableListOf(
            Request().setUpdateCells(
                UpdateCellsRequest()
                    .setRange(gridRange)
                    .setFields("userEnteredFormat.backgroundColor,userEnteredFormat.horizontalAlignment")
                    .setRows(data)
            )
        )

        val request = BatchUpdateSpreadsheetRequest().setRequests(requestList)
        sheetService!!.spreadsheets().batchUpdate(GoogleSpreadSheetConfig.spreadSheetId, request).execute()
    }

    fun updateSpreadSheetLeaderboard(runner: RunNameUser, splitIndex: Int) {
        if(sheetService == null) {
            logger.error("sheet service was not setup correct. Aborted updating leaderboard...")
            return
        }
        if(splitIndex == -1) {
            return
        }
        try {
            logger.info("Starting to update leaderboard")

            var rowIndex = -1
            var columnIndex = -1
            var found = false

            tableContent.forEachIndexed { index, item ->
                val itemLowercase = item.map { it.toString().lowercase() }
                if(itemLowercase.contains(runner.name.lowercase())){
                    columnIndex = index
                    rowIndex = itemLowercase.indexOf(runner.name.lowercase())
                    found = true
                }
            }

            if(splitIndex <= columnIndex && found) {
                logger.info("No new distance PB for ${runner.name}")
                return
            }

            val newColumnIndex = tableContent[splitIndex].map { it.toString().lowercase() }.indexOf(runner.name.lowercase())
            updateCellsBackgroundColor(rowIndex, columnIndex,
                if(newColumnIndex != -1) {
                    newColumnIndex
                } else {
                    tableContent[splitIndex].lastIndex + 1
                },
                splitIndex, runner.chatColor
            )

            if(found) {
                tableContent[columnIndex][rowIndex] = ""
                tableContent[columnIndex].remove("")
                tableContent[columnIndex].add("")
            }

            tableContent[splitIndex].add(runner.name)

            val body: ValueRange = ValueRange()
                .setValues(tableContent)
                .setMajorDimension("COLUMNS")

            sheetService!!.spreadsheets().values().update(GoogleSpreadSheetConfig.spreadSheetId, tableRange, body)
                .setValueInputOption("RAW")
                .execute()


        } catch (e: Exception) {
            logger.error("Updating the google spread sheet failed. ", e)
        }
    }

    private fun transformLetterFromToIndex(input: String): String {
        val output: String
        val columnsNames = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        output = (if(input.filter { it.isLetter() } != "") {
            columnsNames.indexOf(input)
        } else {
            columnsNames[input.toInt()]
        }).toString()

        return output
    }
}
