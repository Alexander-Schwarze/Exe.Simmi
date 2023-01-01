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
import com.google.api.services.sheets.v4.model.ValueRange
import config.GoogleSpreadSheetConfig
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
    private var tableContent = mutableListOf<MutableList<Any>>()

    private val googleCredentialsFilePath = "data\\google_credentials.json"
    private val storedCredentialsTokenFolder = "data\\tokens"

    fun setupConnectionAndLoadData() {
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

            sheetService = Sheets.Builder(httpTransport, jsonFactory, AuthorizationCodeInstalledApp(flow, receiver).authorize("user"))
                .setApplicationName("Sheet Service")
                .build()

        } catch (e: Exception) {
            logger.error("An error occured while setting up connection to google: ", e)
        }

        loadTableContent()
    }

    private fun loadTableContent() {
        if(sheetService == null) {
            logger.error("Sheet Service is not setup. Aborting leaderboard handling... ")
            return
        }
        try {
            val input = sheetService!!.spreadsheets().values()
                .get(GoogleSpreadSheetConfig.spreadSheetId, tableRange)
                .setMajorDimension("COLUMNS")
                .execute()
                .getValues() as MutableList<MutableList<Any>>


            val columnsNames = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            val lastCellLetterIndex =
                columnsNames.indexOf(GoogleSpreadSheetConfig.lastDataCell.filter { it.isLetter() })
            var i = columnsNames.indexOf(GoogleSpreadSheetConfig.firstDataCell.filter { it.isLetter() })
            val startIndex = i
            val output = mutableListOf<MutableList<Any>>()
            while (i <= lastCellLetterIndex) {
                val currentCell =
                    "'${GoogleSpreadSheetConfig.sheetName}'!${columnsNames[i]}${GoogleSpreadSheetConfig.firstDataCell.filter { it.isDigit() }}"
                val cellValue = sheetService!!.spreadsheets().values()
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

            logger.info("Added missing columns to input")
            tableContent = output
        } catch (e: Exception) {
            logger.error("An error occurred while loading the initial table content. Setting table content to an empty list. ", e)
        }
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
            // TODO Set color to cell and remove color of old cell

            var rowIndex = -1
            var columnIndex = -1
            var found = false

            tableContent.forEachIndexed{ index, item ->
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
}
