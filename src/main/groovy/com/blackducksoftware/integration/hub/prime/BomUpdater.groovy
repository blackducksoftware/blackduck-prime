package com.blackducksoftware.integration.hub.prime

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.blackducksoftware.integration.hub.api.generated.view.CodeLocationView
import com.blackducksoftware.integration.hub.exception.DoesNotExistException
import com.blackducksoftware.integration.hub.service.CodeLocationService

@Component
class BomUpdater {
    private final Logger logger = LoggerFactory.getLogger(BomUpdater.class)

    @Autowired
    CodeLocationService codeLocationService

    // we will only attempt to add the code location if it doesn't exist
    public void addCommonsFileUpload() {
        logger.info('starting to add commons-fileupload')
        removeCommonsFileUpload()
        try {
            codeLocationService.getCodeLocationByName('apache commons-fileupload')
        } catch (DoesNotExistException e) {
            String filePath = getClass().getClassLoader().getResource('add-commons-fileupload.jsonld').getFile()
            File jsonldFile = new File(filePath)
            codeLocationService.importBomFile(jsonldFile)
        }

        logger.info('added commons-fileupload, now waiting for bom update')
        boolean codeLocationFoundAndMapped = false
        while (!codeLocationFoundAndMapped) {
            try {
                CodeLocationView codeLocationView = codeLocationService.getCodeLocationByName('apache commons-fileupload')
                codeLocationFoundAndMapped = StringUtils.isNotBlank(codeLocationView.mappedProjectVersion)
            } catch (DoesNotExistException e) {
                logger.warn('The Code Location has not yet been created and mapped.')
                Thread.sleep(Application.FIVE_SECONDS)
            }
        }

        logger.info('Code Location added and mapped')
    }

    public void removeCommonsFileUpload() {
        try {
            CodeLocationView codeLocationView = codeLocationService.getCodeLocationByName('apache commons-fileupload')
            codeLocationService.deleteCodeLocation(codeLocationView)
        } catch (DoesNotExistException e) {
            logger.warn('The Code Location can\'t be removed - it doesn\'t exist!');
        }
    }
}