package composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.DropdownMenuState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.unit.dp
import databaseIndex
import manager
import org.tahomarobotics.scouting.DatabaseManager

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun databaseDropdown(canCreateNew: Boolean) {
    var dropdownMenuState = false
    var newDatabaseName = ""
    Button(
        onClick = {
            dropdownMenuState = !dropdownMenuState
        }
    ) {
        Text(databaseIndex.value)
    }
    ExposedDropdownMenuBox(
        expanded = dropdownMenuState,
        onExpandedChange = { it ->
            dropdownMenuState = it
        },
        modifier = Modifier
            .width(200.dp)
            .padding(15.dp),
    ) {
        DropdownMenu(
            expanded = dropdownMenuState,
            onDismissRequest = {
                dropdownMenuState = !dropdownMenuState
            }
        ) {
            manager.forEach { database ->
                DropdownMenuItem(
                    onClick = {
                        databaseIndex.value = database.key
                        dropdownMenuState = false
                    },
                ) { Text(database.key) }
            }
            if (canCreateNew) {
                Row {
                    TextField(
                        value = newDatabaseName,
                        onValueChange = {
                            newDatabaseName = it
                        },
                        modifier = Modifier.fillMaxWidth(7 / 8f)
                    )
                    Button(
                        onClick = {
                            manager.set(newDatabaseName, DatabaseManager(2025))
                        }
                    )
                    {
                        Text("+")
                    }
                }
            }
        }
    }
}


//ExposedDropdownMenuBox(
//                    modifier = Modifier
//                        .width(200.dp)
//                        .padding(15.dp)
//                        .align(Alignment.CenterEnd),
//                    expanded = dropDownExpanded,
//                    onExpandedChange = { it ->
//                        dropDownExpanded = it
//                    }
//                ) {
//                    TextField(
//                        modifier = Modifier
//                            .menuAnchor(),
//                        value = driveType.value,
//                        onValueChange = {},
//                        readOnly = true,
//                        trailingIcon = {
//                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropDownExpanded)
//                        },
//                        textStyle = TextStyle(color = Color.White)
//                    )
//                    DropdownMenu(
//                        expanded = dropDownExpanded,
//                        onDismissRequest = {
//                            dropDownExpanded = false
//                        }
//                    ) {
//                        HorizontalDivider(
//                            color = getCurrentTheme().onSurface,
//                            thickness = 3.dp
//                        )
//
//                        DropdownMenuItem(
//                            {
//                                Text(
//                                    text = "Swerve",
//                                    color = Color.White
//                                )
//                            },
//                            onClick = {
//                                driveType.value = "Swerve"
//                                dropDownExpanded = false
//                            }
//                        )
//
//                        HorizontalDivider(
//                            color = getCurrentTheme().onSurface,
//                            thickness = 3.dp
//                        )
//
//                        DropdownMenuItem(
//                            {
//                                Text(
//                                    text = "Tank",
//                                    color = Color.White
//                                )
//                            },
//                            onClick = {
//                                driveType.value = "Tank"
//                                dropDownExpanded = false
//                            }
//                        )
//
//                        HorizontalDivider(
//                            color = getCurrentTheme().onSurface,
//                            thickness = 3.dp
//                        )
//
//                        DropdownMenuItem(
//                            {
//                                Text(
//                                    text = "Mecanum",
//                                    color = Color.White
//                                )
//                            },
//                            onClick = {
//                                driveType.value = "Mecanum"
//                                dropDownExpanded = false
//                            }
//                        )
//
//                        HorizontalDivider(
//                            color = getCurrentTheme().onSurface,
//                            thickness = 3.dp
//                        )
//
//                        DropdownMenuItem(
//                            {
//                                Text(
//                                    text = "Other",
//                                    color = Color.White
//                                )
//                            },
//                            onClick = {
//                                driveType.value = "Other"
//                                dropDownExpanded = false
//                            }
//                        )
//
//                        HorizontalDivider(
//                            color = getCurrentTheme().onSurface,
//                            thickness = 3.dp
//                        )
//
//                    }